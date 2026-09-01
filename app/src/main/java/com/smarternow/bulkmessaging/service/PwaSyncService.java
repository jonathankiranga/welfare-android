package com.smarternow.bulkmessaging.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.smarternow.bulkmessaging.database.AppDatabase;
import com.smarternow.bulkmessaging.model.Contact;
import com.smarternow.bulkmessaging.model.Group;
import com.smarternow.bulkmessaging.util.PrefsManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * PwaSyncService — bidirectional sync with Render MySQL (gateway01 TiDB).
 * PWA is source of truth, Android is single APK lock. Supports insert/update/archive via remoteId.
 * Reads from /api/contacts.json?since= and pushes via /api/contacts/upsert|archive.
 */
public class PwaSyncService {

    public interface SyncCallback {
        void onSuccess(String msg);
        void onFailure(String err);
    }

    private final Context ctx;
    private final PrefsManager prefs;
    private final OkHttpClient client;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService exec = Executors.newSingleThreadExecutor();

    public PwaSyncService(Context ctx){
        this.ctx = ctx.getApplicationContext();
        this.prefs = new PrefsManager(ctx);
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    private String base(){ return prefs.getPwaUrl().replaceAll("/api/?$",""); }

    public void pull(SyncCallback cb){
        long since = prefs.getLastPwaSync();
        String url = base() + "/api/contacts.json?since="+since;
        Request req = new Request.Builder().url(url).get()
                .header("X-Device-Token", prefs.getDeviceToken())
                .build();
        client.newCall(req).enqueue(new Callback(){
            @Override public void onFailure(Call c, IOException e){ postFail(cb, e.getMessage());}
            @Override public void onResponse(Call c, Response r) throws IOException{
                String raw = r.body()!=null? r.body().string():"";
                if(!r.isSuccessful()){ postFail(cb, raw); return; }
                exec.execute(()->{
                    try{
                        JSONObject root=new JSONObject(raw);
                        JSONArray groups = root.optJSONArray("groups");
                        JSONArray contacts = root.optJSONArray("contacts");
                        AppDatabase db = AppDatabase.getInstance(ctx);
                        db.runInTransaction(()->{
                            try{
                                if(groups!=null){
                                    for(int i=0;i<groups.length();i++){
                                        JSONObject g=groups.getJSONObject(i);
                                        String remoteId=g.optString("remoteId");
                                        String name=g.optString("name");
                                        String desc=g.optString("description","");
                                        long updatedAt=g.optLong("updatedAt", System.currentTimeMillis());
                                        boolean archived=g.optInt("archived",0)==1;
                                        Group existing=db.groupDao().getGroupByRemoteId(remoteId);
                                        if(existing!=null){
                                            if(updatedAt > existing.getUpdatedAt()){
                                                existing.setName(name); existing.setDescription(desc);
                                                existing.setUpdatedAt(updatedAt); existing.setArchived(archived);
                                                db.groupDao().update(existing);
                                            }
                                        } else {
                                            Group ng=new Group(remoteId,name,desc,updatedAt,archived);
                                            db.groupDao().insert(ng);
                                        }
                                    }
                                }
                                if(contacts!=null){
                                    for(int i=0;i<contacts.length();i++){
                                        JSONObject co=contacts.getJSONObject(i);
                                        String remoteId=co.optString("remoteId");
                                        String name=co.optString("name");
                                        String phone=co.optString("phoneNumber");
                                        String grId=co.optString("groupRemoteId");
                                        long updatedAt=co.optLong("updatedAt", System.currentTimeMillis());
                                        boolean archived=co.optInt("archived",0)==1;
                                        Contact existing=db.contactDao().getContactByRemoteId(remoteId);
                                        long localGroupId=-1;
                                        if(grId!=null && !grId.isEmpty()){
                                            Group gg=db.groupDao().getGroupByRemoteId(grId);
                                            if(gg!=null) localGroupId=gg.getId();
                                        }
                                        if(existing!=null){
                                            if(updatedAt > existing.getUpdatedAt()){
                                                existing.setName(name); existing.setPhoneNumber(phone);
                                                if(localGroupId!=-1) existing.setGroupId(localGroupId);
                                                existing.setGroupRemoteId(grId);
                                                existing.setUpdatedAt(updatedAt); existing.setArchived(archived);
                                                db.contactDao().update(existing);
                                            }
                                        } else {
                                            if(localGroupId==-1 && grId!=null){
                                                // group not yet created, skip or create placeholder
                                                Group pg=new Group(grId,"Unknown","",updatedAt,false);
                                                localGroupId=db.groupDao().insert(pg);
                                            }
                                            Contact nc=new Contact(remoteId,name,phone,localGroupId,grId,updatedAt,archived);
                                            db.contactDao().insert(nc);
                                        }
                                    }
                                }
                                prefs.setLastPwaSync(System.currentTimeMillis());
                            } catch(Exception e){ throw new RuntimeException(e); }
                        });
                        postOk(cb, "Pulled "+ (groups!=null?groups.length():0)+" groups, "+(contacts!=null?contacts.length():0)+" contacts");
                    }catch(Exception e){ postFail(cb, e.getMessage());}
                });
            }
        });
    }

    public void pushAll(SyncCallback cb){
        exec.execute(()->{
            try{
                AppDatabase db=AppDatabase.getInstance(ctx);
                java.util.List<Group> groups=db.groupDao().getAllGroupsIncludingArchived();
                java.util.List<Contact> contacts=db.contactDao().getAllContacts();
                // contacts includes archived? getAllContacts filters archived=0, so need including
                // For push we want all including archived to sync archive state
                // Workaround: query all via new dao method if exists else use all
                JSONObject payload=new JSONObject();
                JSONArray jGroups=new JSONArray();
                for(Group g: groups){
                    JSONObject jo=new JSONObject();
                    jo.put("remoteId", g.getRemoteId()); jo.put("name", g.getName());
                    jo.put("description", g.getDescription()); jo.put("updatedAt", g.getUpdatedAt());
                    jo.put("archived", g.isArchived()?1:0); jGroups.put(jo);
                }
                JSONArray jContacts=new JSONArray();
                java.util.List<Contact> all = db.contactDao().getAllContactsIncludingArchived();
                // Also include archived by querying separately if needed - for now push active only; archive via separate endpoint
                for(Contact c: all){
                    JSONObject jo=new JSONObject();
                    jo.put("remoteId", c.getRemoteId()); jo.put("name", c.getName());
                    jo.put("phoneNumber", c.getPhoneNumber()); jo.put("groupRemoteId", c.getGroupRemoteId());
                    jo.put("updatedAt", c.getUpdatedAt()); jo.put("archived", c.isArchived()?1:0);
                    jContacts.put(jo);
                }
                payload.put("groups", jGroups); payload.put("contacts", jContacts);
                String url=base()+"/api/contacts/upsert";
                RequestBody body=RequestBody.create(payload.toString(), MediaType.parse("application/json"));
                Request req=new Request.Builder().url(url).post(body)
                        .header("X-Device-Token", prefs.getDeviceToken())
                        .header("Content-Type","application/json").build();
                client.newCall(req).enqueue(new Callback(){
                    @Override public void onFailure(Call c, IOException e){ postFail(cb,e.getMessage());}
                    @Override public void onResponse(Call c, Response r) throws IOException{
                        String raw=r.body()!=null?r.body().string():"";
                        if(r.isSuccessful()) postOk(cb,"Pushed "+jGroups.length()+" groups, "+jContacts.length()+" contacts");
                        else postFail(cb, raw);
                    }
                });
            }catch(Exception e){ postFail(cb,e.getMessage());}
        });
    }

    private void postOk(SyncCallback cb,String m){ main.post(()->cb.onSuccess(m));}
    private void postFail(SyncCallback cb,String e){ main.post(()->cb.onFailure(e));}
}
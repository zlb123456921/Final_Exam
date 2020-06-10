package com.example.final_exam;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements Runnable  {
    private String updateDate="";
    TextView input;
    Handler my_handler;
    private ListView mListView;
    ListAdapter adapter;
    Map<String,String> selected;
    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        input=(TextView)findViewById(R.id.editText12);
        mListView = (ListView) findViewById(R.id.result_box);
        //列表元素点击事件
/*
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String listkey = (String) mListView.getItemAtPosition(position);
                String url=selected.get(listkey);
                //屌用浏览器
                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                Log.i("ListViewListener==>",url);
            }
        });
*/


//判断数据是否需要更新
        SharedPreferences sp=getSharedPreferences("SchoolReport", Activity.MODE_PRIVATE);
        updateDate=sp.getString("update_date","");
        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        final String todayStr=sdf.format(today);
        if (updateDate.equals("")){
            Thread my_Thread=new Thread(this);
            my_Thread.start();
            Log.i("date","启动更新数据");

        }else{



            Date todaydate = null;
            Date olddate=null;
            try {
                todaydate = sdf.parse(todayStr);
                olddate=sdf.parse(updateDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            long ts = todaydate.getTime();
            long os= olddate.getTime();
            if((ts-os)>604800017){//--------------------------------测试时为小于号
                Thread my_Thread=new Thread(this);
                my_Thread.start();
                Log.i("date","启动更新数据"+ts+"\t"+os);
            }else{
                Log.i("date","无需启动更新数据");
            }
        }


//判断日期是否相差一周
        my_handler=new Handler(){
            @Override
            public  void handleMessage(Message msg){
                super.handleMessage(msg);
                if(msg.what==5){

                    HashMap<String,String> recv=(HashMap<String, String>)msg.obj;


                    SharedPreferences sp=getSharedPreferences("SchoolReport", Activity.MODE_PRIVATE);
//                    SharedPreferences sp=PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor myEdt= sp.edit();
                    myEdt.putString("update_date",todayStr);

                    for (String key : recv.keySet()) {
                        myEdt.putString(key,recv.get(key));
//                        Log.i("Share==>",key+"===存储成功"+recv.get(key));
                    }
                    myEdt.commit();

                }
            }

        };
    }

    public void onClick4(View m_view){
        String searchInput=(String)input.getText().toString();
        if(searchInput.isEmpty()==true){
            Toast.makeText(this,"内容不能为空",Toast.LENGTH_SHORT).show();
            return;
        }
        SharedPreferences sp=getSharedPreferences("SchoolReport", Activity.MODE_PRIVATE);
        Map<String,String> result= (Map<String, String>) sp.getAll();
        if (sp.getAll().isEmpty()){
            Toast.makeText(this,"请等待，第一次使用需加载数据",Toast.LENGTH_SHORT).show();
            return;
        }
        selected=new HashMap<>();
        List<String> listKey=new ArrayList<>();
        result.remove("update_date");
        for (String key : result.keySet()) {
         //   Log.i("SharedPreferences==>",key+"==="+result.get(key));
            if(key.indexOf(searchInput)>=0){
                selected.put(key,result.get(key));
                listKey.add(result.get(key));
              //  Log.i("result==>",key+"===添加结果数据成功"+result.get(key));
            }
        }

        if (selected.isEmpty()) {
            Toast.makeText(this,"未找到相关通知，请尝试搜索较少的关键字",Toast.LENGTH_SHORT).show();
            return;
        }else{
            ListAdapter adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,listKey);
            mListView.setAdapter(adapter);
        }
    }


    @Override
    public void run() {
        Bundle mbd=new Bundle();
        HashMap<String,String> map =new HashMap<>();
        Document doc = null;
        Document mainWeb = null;
        String url="http://www.boohee.com/food/";
        String startURL="http://www.boohee.com/food/group/1";
        boolean hasNext=false;

        try {
            mainWeb = Jsoup.connect(url).timeout(50000000).maxBodySize(0).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Elements mainUl=mainWeb.select("ul.row");
        Elements liList=mainUl.first().select("li");
        int sum=liList.size();
        Log.i("jSoup", String.valueOf(sum));
        for (int j = 1; j <=sum ; j++) {
            startURL=startURL.substring(0,startURL.lastIndexOf("/")+1)+j;
            Log.i("jSoup", startURL);

            do {
                try {
                    doc = Jsoup.connect(startURL).timeout(50000000).maxBodySize(0).get();
                    //通过延迟50000000毫秒,设置响应body不限制
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Elements ul = doc.select("ul.food-list");
                Elements li = (ul.get(0)).select("li");

                for (Element i : li) {
                    Element a = i.select("h4").first().select("a").first();
                    Element p = i.select("p").first();
                    // String linkHref = a.attr("href");
                    String title = a.attr("title");
                    String number = p.text();
                    String result=title+"=======>"+number;
                  //  Log.i("jsoup",result);
                    map.put(title, result);
                }
                Elements next=doc.select("a:contains(下一页)");
                hasNext=!next.isEmpty();
                if(hasNext) {
                    String index=next.get(0).attr("href");
                    index=index.substring(index.indexOf('?'));
                    int endIndex=startURL.indexOf("?")==-1 ? startURL.length():startURL.indexOf("?");
                    startURL=startURL.substring(0,endIndex)+index;
                 //   Log.i("jsoup", startURL);
                } else {
                    Log.i("jsoup===>","not found");
                    break;
                }
            }while (hasNext);
        }



        /*try {
            boolean hasNext=false;

            do {
                doc = Jsoup.connect(startURL).timeout(50000000).maxBodySize(0).get();
                //通过延迟50000000毫秒,设置响应body不限制

                Elements ul = doc.select("ul.whitenewslist.clearfix");
                Elements li = (ul.get(0)).select("li");

                for (Element i : li) {
                    Element a = i.select("a").first();
                    String linkHref = a.attr("href");
                    String title = a.attr("title");
                    linkHref = url + linkHref.substring(2);
                    map.put(title, linkHref);
                }

                Elements next=doc.select("a:contains(下页)");
                hasNext=!next.isEmpty();
                if(hasNext) {
                    String index=next.get(0).attr("href");
                    startURL=startURL.substring(0,startURL.lastIndexOf('/')+1)+index;
                    Log.i("jsoup", startURL);
                } else {
                    Log.i("jsoup===>","not found");
                    break;
                }
            }while (hasNext);

        } catch (IOException e) {
            e.printStackTrace();
        }*/

        Message msg=my_handler.obtainMessage(5);
        msg.obj=map;
        my_handler.sendMessage(msg);

    }
}
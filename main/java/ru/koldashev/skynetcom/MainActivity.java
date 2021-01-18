package ru.koldashev.skynetcom;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.Calendar;

import ru.koldashev.skynetcom.receivers.NetworkChangeReceiver;

//импорт из есивера проверяющего связь

public class MainActivity extends AppCompatActivity {
    //переменные для проверки связи
    private BroadcastReceiver mNetworkReceiver;
    //static TextView tv_check_connection;

    private static final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 99;
    static WebView mywebView;
    public static final String NOTIFICATION_CHANNEL_ID = "10001" ;
    //private final static String default_notification_channel_id = "default" ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //инициализация провки связи
        //tv_check_connection=(TextView) findViewById(R.id.tv_check_connection);//инициализация информера наличия сети
        mNetworkReceiver = new NetworkChangeReceiver();
        registerNetworkBroadcastForNougat();
        //загрузка страницы
        mywebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings= mywebView.getSettings();


        //процедура проверяющая наличие разрешения
        int permissionStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            //если доступ есть, то делаем что то
            mywebView.setWebChromeClient(new WebChromeClient(){
                public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                    callback.invoke(origin, true, false);
                }
            });

        } else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_ACCESS_FINE_LOCATION);
        }
        // конец проверки разрешения
        mywebView.getSettings().setMediaPlaybackRequiresUserGesture(false);//разрешаем играть музыку
        mywebView.getSettings().setAppCacheEnabled(false);
        mywebView.getSettings().setDatabaseEnabled(true);
        mywebView.getSettings().setDomStorageEnabled(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        // Line of Code for opening links in app
        mywebView.loadUrl("https://skynetcom.koldashev.ru");
        //грузим со страницы яваскрипт
        mywebView.addJavascriptInterface(new JavaScriptInterface(this), "AndroidFunction");

        //загрузка страницы
        mywebView.setWebViewClient(new WebViewClient(){});
        /*mywebView.setWebViewClient(new WebViewClient(){

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                // Handle the error
                mywebView.loadUrl("file:///android_asset/errorpage.html");
            }
            @TargetApi(android.os.Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                // Redirect to deprecated method, so you can use it in all SDK versions

                super.onReceivedError(view, request, error);
                mywebView.loadUrl("file:///android_asset/errorpage.html");

            }

            @Override
            public void onReceivedHttpError(WebView view,
                                            WebResourceRequest request, WebResourceResponse errorResponse) {

                super.onReceivedHttpError(view, request, errorResponse);
                mywebView.loadUrl("file:///android_asset/errorpage.html");
            }
        });*/
        //проверка наличия интернета

    }

    //поток проверяющий наличие разрешения
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted
                    mywebView.setWebChromeClient(new WebChromeClient(){
                        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                            callback.invoke(origin, true, false);
                        }
                    });
                    //разрешили, запускаем разрешенную опцию
                } else {
                    // permission denied
                    //не разрешили, убиваем себя
                }
                return;
        }
    }

    //создаем уведомлялку
    private void scheduleNotification (Notification notification , int delay/*указываем время через которое надо появиться уведомлению*/, int numberNotyfy) {
        Intent notificationIntent = new Intent(this, MyNotificationPublisher.class);
        notificationIntent.putExtra(MyNotificationPublisher.NOTIFICATION_ID, numberNotyfy);//меняем номер уведомления
        notificationIntent.putExtra(MyNotificationPublisher.NOTIFICATION, notification);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, numberNotyfy, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        long futureInMillis = System.currentTimeMillis() + delay;
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        assert alarmManager != null;
        alarmManager.set(AlarmManager.RTC_WAKEUP, futureInMillis, pendingIntent);
        }

    //создаем канал для трансляции уведомлений и настраиваем вид уведомлений
    private Notification getNotification (String content, String notId){
        // Создаем активность по нажатию на наше уведомление
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder( this, notId) ;
        builder.setContentTitle(content) ;
        builder.setContentIntent(pendingIntent);//запускаем приложение
        builder.setContentText("Есть задачи требующие решения ") ;
        builder.setSmallIcon(R.drawable. ic_launcher_foreground ) ;
        builder.setAutoCancel( true ) ;
        builder.setChannelId( NOTIFICATION_CHANNEL_ID ) ;
        return builder.build();
    }

    //подгружаем JS со страницы

    public class JavaScriptInterface {

        Context mContext;

        JavaScriptInterface(Context c) {
            mContext = c;
          }

        @JavascriptInterface
        public void showToast(int CallIs) {
             if(CallIs == 1){
                //останавливаем уведомлялку
                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.cancelAll();
                //определяем какой час и какой день недели
                 Calendar TimeNow = Calendar.getInstance();
                 final int currentTime = TimeNow.get(Calendar.HOUR_OF_DAY); //0-23
                 final int currentDayWeek = TimeNow.get(Calendar.DAY_OF_WEEK);//1-7 первый день воскресение
                 if(currentDayWeek != 7 & currentDayWeek != 0) {
                     //делаем уведомления
                     switch (currentTime) {
                         case 9:
                             for(int CountN=1;CountN<9;CountN++){
                                 scheduleNotification(getNotification("Вас небыло "+CountN+" час. Проверьте заявки!", "id"+CountN), CountN*3600000, CountN);
                              }

                         case 10:
                             for(int CountN=1;CountN<8;CountN++){
                                 scheduleNotification(getNotification("Вас небыло "+CountN+" час. Проверьте заявки!", "id"+CountN), CountN*3600000, CountN);
                             }

                         case 11:

                             for(int CountN=1;CountN<7;CountN++){
                                 scheduleNotification(getNotification("Вас небыло "+CountN+" час. Проверьте заявки!", "id"+CountN), CountN*3600000, CountN);
                             }

                         case 12:

                             for(int CountN=1;CountN<6;CountN++){
                                 scheduleNotification(getNotification("Вас небыло "+CountN+" час. Проверьте заявки!", "id"+CountN), CountN*3600000, CountN);
                             }

                         case 13:
                             for(int CountN=1;CountN<5;CountN++){
                                 scheduleNotification(getNotification("Вас небыло "+CountN+" час. Проверьте заявки!", "id"+CountN), CountN*3600000, CountN);
                             }

                         case 14:
                             for(int CountN=1;CountN<4;CountN++){
                                 scheduleNotification(getNotification("Вас небыло "+CountN+" час. Проверьте заявки!", "id"+CountN), CountN*3600000, CountN);
                             }

                         case 15:
                             for(int CountN=1;CountN<3;CountN++){
                                 scheduleNotification(getNotification("Вас небыло "+CountN+" час. Проверьте заявки!", "id"+CountN), CountN*3600000, CountN);
                             }

                         case 16:
                             scheduleNotification(getNotification("Скоро конец рабочего дня! Время проветить заявки!", "id1"), 3600000, 1);

                         default:
                             //уведомление на утро
                             if(currentDayWeek != 6){scheduleNotification(getNotification("Поздравляем! Наступил новый рабочий день. Проверьте свои задачи.", "id111"), (23-currentTime+9)*3600000, 111);}
                             if(currentDayWeek == 6){scheduleNotification(getNotification("Поздравляем! Наступил новый рабочий день. Проверьте свои задачи.", "id111"), (23-currentTime+9)*3600000*3, 111);}

                        }
                    } else if(currentDayWeek == 7){
                     scheduleNotification(getNotification("Поздравляем! Наступил первый рабочий день. Проверьте свои задачи.", "id1"), (23-currentTime+9)*2*3600000, 1); //если суббота напоминаем в понедельник утром
                 }else if(currentDayWeek == 0){
                     scheduleNotification(getNotification("Поздравляем! Наступил первый рабочий день. Проверьте свои задачи.", "id1"), (23-currentTime+9)*3600000, 1); //если воскресение напоминаем в понедельник утром
                 }

            } else if(CallIs == 0){
                //останавливаем уведомлялку
                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.cancelAll();
            }
        }
    }

    public static void dialog(boolean value){

        if(value){

            //tv_check_connection.setText("Подключение восстановлено");


            Handler handler = new Handler();
            Runnable delayrunnable = new Runnable() {
                @Override
                public void run() {
                    //tv_check_connection.setVisibility(View.GONE);// как то влиеяет на контент
                    //отображаем наш сайтик
                    mywebView.loadUrl("https://skynetcom.koldashev.ru");
                }
            };
            handler.postDelayed(delayrunnable, 3000);// проверка каждые три секунды
        }else {
            //tv_check_connection.setVisibility(View.VISIBLE);
            //tv_check_connection.setText("Подключение к интернет потеряно");
            //выводим что то
            mywebView.loadUrl("file:///android_asset/errorpage.html");
        }
    }


    private void registerNetworkBroadcastForNougat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerReceiver(mNetworkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            registerReceiver(mNetworkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    protected void unregisterNetworkChanges() {
        try {
            unregisterReceiver(mNetworkReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterNetworkChanges();
    }


}

package com.ekibastuz.net;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.MailTo;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class ActivityShomla extends AppCompatActivity {

    private static final int MULTIPLE_PERMISSIONS_REQUEST_CODE = 1;
    WebView shomweb;
    private static final int FILE_CHOOSER_RESULT_CODE = 2;
    private ValueCallback<Uri[]> filePathCallback;
    private String cameraPhotoPath;
    private String pushToken;
    private ConstraintLayout imageView2;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);




        FirebaseApp.initializeApp(this);
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        pushToken = task.getResult();
                        Log.d("FCM", "FCM Registration Token: " + pushToken);

                        // Установите куки с pushToken в WebView
                        if (pushToken != null) {
                            setCookieWithPushToken(pushToken);
                        }
                    }
                });

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        String cookies = cookieManager.getCookie("https://ekibastuz.net/"); // Замените на свой URL
        Log.d(TAG, "Cookies: " + cookies);


        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setStatusBarColor();
        setContentView(R.layout.shomlaactivity);

        // Инициализируем imageView2
        imageView2 = findViewById(R.id.imageView3);


        shomweb = findViewById(R.id.shomweb);

        configureWebViewSettings();

        setWebChromeClient();

        setWebViewClient();

        loadInitialUrl();

        // Запрос разрешений на доступ к геолокации, камере и микрофону
        // Проверяем версию операционной системы
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Версия Android 13 (API 33) и выше
            requestPermissionsWithNotificationPermission();
        } else {
            // Версия Android ниже 13
            requestRegularPermissions();
        }


        if (!isNetworkAvailable()) {
            showNoInternetScreen();
        }

        FirebaseMessaging.getInstance().subscribeToTopic("all")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String msg = task.isSuccessful() ? "Subscribed to topic" : "Subscription to topic failed";
                        Log.d(TAG, msg);
                    }
                });

        networkChangeReceiver = new NetworkChangeReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, filter);
    }

    private void setCookieWithPushToken(String token) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            cookieManager.setCookie("https://ekibastuz.net/", "push_token=" + token);
            cookieManager.flush();
        } else {
            CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(this);
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            cookieManager.setCookie("https://ekibastuz.net/", "push_token=" + token);
            cookieSyncManager.sync();
        }

        Log.d("PushToken", "Received push token: " + token);
        // Используйте JavaScript для установки куки в WebView
        shomweb.evaluateJavascript("document.cookie = 'push_token=" + token + "';", null);
    }

    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.black));
        }
    }

    private void configureWebViewSettings() {
        WebSettings webSettings = shomweb.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setUseWideViewPort(true);

        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        WebView.setWebContentsDebuggingEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + "; " + Build.MODEL +
                ") AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Mobile Safari/537.36");
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
    }

    private void setWebChromeClient() {
        shomweb.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                showAlertDialog(message, result);
                return true;
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("WebViewError", consoleMessage.message());
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                showConfirmDialog(message, result);
                return true;
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (ActivityShomla.this.filePathCallback != null) {
                    ActivityShomla.this.filePathCallback.onReceiveValue(null);
                }
                ActivityShomla.this.filePathCallback = filePathCallback;

                Log.d("FileChooser", "Открытие камеры для захвата фото");

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        Log.e("WebView", "Не удалось создать файл изображения", ex);
                    }
                    if (photoFile != null) {
                        cameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                        Uri photoURI = FileProvider.getUriForFile(ActivityShomla.this, "com.ekibastuz.net.fileprovider", photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } else {
                        takePictureIntent = null;
                    }
                }

                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("image/*");

                Intent[] intentArray = takePictureIntent != null ? new Intent[]{takePictureIntent} : new Intent[0];

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Выбор изображения");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

                startActivityForResult(chooserIntent, FILE_CHOOSER_RESULT_CODE);

                return true;
            }
        });
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (filePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }
            Uri[] results = null;

            if (resultCode == Activity.RESULT_OK) {
                Log.d("FileChooser", "Фото успешно сделано");

                if (data == null || data.getData() == null) {
                    if (cameraPhotoPath != null) {
                        results = new Uri[]{Uri.parse(cameraPhotoPath)};
                        Log.d("FileChooser", "Фото путь: " + cameraPhotoPath);
                    }
                } else {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                        Log.d("FileChooser", "Фото выбрано из галереи: " + dataString);
                    }
                }
            } else {
                Log.e("FileChooser", "Фото не было сделано или выбрано");
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
        if (shomweb.canGoBack()) {
            shomweb.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveCookies();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCookies();
    }

    private void saveCookies() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.flush();
        } else {
            CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(this);
            cookieSyncManager.sync();
        }
    }

    private void loadCookies() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
        } else {
            CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(this);
            cookieSyncManager.startSync();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkChangeReceiver != null) {
            unregisterReceiver(networkChangeReceiver);
        }
    }

    private void showAlertDialog(String message, final JsResult result) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ActivityShomla.this);
        builder.setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result.confirm();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void showConfirmDialog(String message, final JsResult result) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ActivityShomla.this);
        builder.setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result.confirm();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result.cancel();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void setWebViewClient() {
        shomweb.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.d("WebViewError", "Error Code: " + errorCode + " Description: " + description);
                if (description.contains("camera")) {
                    Log.e("WebViewError", "Camera error: There was an issue with accessing the camera.");
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                int statusCode = errorResponse.getStatusCode();
                Log.d("WebViewError", "HTTP Error Code: " + statusCode);
                if (errorResponse.getReasonPhrase().toLowerCase().contains("camera")) {
                    Log.e("WebViewError", "Camera error: There was an issue with accessing the camera.");
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("tel:")) {
                    handlePhoneCall(url);
                    return true;
                }

                if (url.startsWith("mailto:")) {
                    handleEmailLink(url);
                    return true;
                }
                if (url.startsWith("mail:")) {
                    handleEmailLink(url);
                    return true;
                }

                if (url.startsWith("whatsapp:")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(view.getContext(), "Приложение WhatsApp не установлено", Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
                if (url.startsWith("tg:")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(view.getContext(), "Приложение Telegram не установлено", Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
                if (url.startsWith("viber:")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(view.getContext(), "Приложение Viber не установлено", Toast.LENGTH_LONG).show();
                    }
                    return true;
                }

                if (url.startsWith("intent://filmDetail")){
                    Uri uri = Uri.parse("https://www.kinopoisk.ru/");
                    openInCustomTab(uri);
                    return true;
                }
                if (url.startsWith("intent://vk.com")){
                    Uri uri = Uri.parse("https://m.vk.com/pro_portal");
                    try {
                    Intent insta = new Intent(Intent.ACTION_VIEW, uri);
                    insta.setPackage("com.vkontakte.android");
                    startActivity(insta);
                    } catch (ActivityNotFoundException e) {
                        openInCustomTab(uri);
                        }
                    return true;
                }
                if (url.startsWith("intent://?referrer")){
                    Uri uri = Uri.parse("https://yandex.com/maps/");
                    openInCustomTab(uri);
                    return true;
                }
                if (url.startsWith("intent://route?")){
                    Uri uri = Uri.parse("https://taxi.yandex.ru");
                    openInCustomTab(uri);
                    return true;
                }

                if (url.startsWith("intent://qr.vk.com")){
                    Toast.makeText(view.getContext(), "Нет возможности войти через приложение ВК", Toast.LENGTH_LONG).show();
                    return true;
                }


                view.loadUrl(url);
                return true;
            }
        });
    }

    private void openInCustomTab(Uri uri) {
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        customTabsIntent.launchUrl(this, uri);
    }
    private void handlePhoneCall(String url) {
        Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
        startActivity(dialIntent);
    }

    private void handleEmailLink(String url) {
        MailTo mailTo = MailTo.parse(url);
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{mailTo.getTo()});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, mailTo.getSubject());
        emailIntent.putExtra(Intent.EXTRA_CC, mailTo.getCc());
        emailIntent.putExtra(Intent.EXTRA_TEXT, mailTo.getBody());
        emailIntent.setType("message/rfc822");
        startActivity(emailIntent);
    }

    private void loadInitialUrl() {
        shomweb.loadUrl("https://ekibastuz.net/");
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    private void requestRegularPermissions() {
        // Запрос обычных разрешений
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA};
        ActivityCompat.requestPermissions(this, permissions, MULTIPLE_PERMISSIONS_REQUEST_CODE);
    }

    private void requestPermissionsWithNotificationPermission() {
        // Запрос разрешений, включая POST_NOTIFICATIONS
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.POST_NOTIFICATIONS};
        ActivityCompat.requestPermissions(this, permissions, MULTIPLE_PERMISSIONS_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MULTIPLE_PERMISSIONS_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                // Все разрешения получены
                loadInitialUrl();
            } else {
                // Одно или несколько разрешений не были предоставлены
                // Выводим уведомление и закрываем приложение
                Toast.makeText(this, "Не все разрешения были предоставлены. Приложение будет закрыто.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    private void showNoInternetScreen() {
        setContentView(R.layout.nointernet);
    }

    private void showWebViewScreen() {
        setContentView(R.layout.shomlaactivity);
        shomweb = findViewById(R.id.shomweb);
        configureWebViewSettings();
        setWebChromeClient();
        setWebViewClient();
        loadInitialUrl();
    }

    private class NetworkChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

            boolean isConnected = networkInfo != null && networkInfo.isConnected();
            if (isConnected && !isNetworkConnected) {
                isNetworkConnected = true;
                showWebViewScreen();
            } else if (!isConnected && isNetworkConnected) {
                isNetworkConnected = false;
                showNoInternetScreen();
            }
        }
    }

    private boolean isNetworkConnected = true;
    private NetworkChangeReceiver networkChangeReceiver;
}


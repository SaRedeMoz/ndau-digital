// ═══════════════════════════════════════════════════════════════
//  NDAU DIGITAL — APP ANDROID COMPLETA
//  Linguagem: Java  |  Min SDK: 24 (Android 7.0)
//  Base de dados: Firebase Firestore + Storage
// ═══════════════════════════════════════════════════════════════

// ══════════════════════════════════════════
//  FICHEIRO 1: app/build.gradle
// ══════════════════════════════════════════
/*
plugins {
    id 'com.android.application'
    id 'com.google.gms.google-services'  // Firebase
}

android {
    compileSdk 34
    defaultConfig {
        applicationId "mz.ndau.digital"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0.0"
    }
    buildFeatures { viewBinding true }
}

dependencies {
    // Firebase BOM — controla versões automaticamente
    implementation platform('com.google.firebase:firebase-bom:32.7.0')
    implementation 'com.google.firebase:firebase-auth'
    implementation 'com.google.firebase:firebase-firestore'
    implementation 'com.google.firebase:firebase-storage'
    implementation 'com.google.firebase:firebase-analytics'

    // UI
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.navigation:navigation-fragment:2.7.6'
    implementation 'androidx.navigation:navigation-ui:2.7.6'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    implementation 'de.hdodenhof:circleimageview:3.1.0'
}
*/

// ══════════════════════════════════════════
//  FICHEIRO 2: AndroidManifest.xml
// ══════════════════════════════════════════
/*
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="mz.ndau.digital">

    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.RECORD_AUDIO"/>
    <uses-permission android:name="android.permission.VIBRATE"/>

    <application
        android:name=".NdauApp"
        android:icon="@mipmap/ic_launcher"
        android:label="Ndau Digital"
        android:theme="@style/Theme.NdauDigital"
        android:usesCleartextTraffic="false">

        <activity android:name=".ui.SplashActivity"
            android:exported="true"
            android:theme="@style/Theme.NdauDigital.Splash">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
        <activity android:name=".ui.AuthActivity" android:exported="false"/>
        <activity android:name=".ui.MainActivity" android:exported="false"/>
    </application>
</manifest>
*/

// ══════════════════════════════════════════
//  FICHEIRO 3: NdauApp.java (Application)
// ══════════════════════════════════════════
package mz.ndau.digital;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class NdauApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}

// ══════════════════════════════════════════
//  FICHEIRO 4: models/User.java
// ══════════════════════════════════════════
package mz.ndau.digital.models;

public class User {
    public String uid, name, email, phone, ndauLevel;
    public long totalWords, totalVoice;
    public double pendingMT, earnedMT, withdrawnMT;

    public User() {} // necessário para Firebase

    public User(String uid, String name, String email, String phone, String ndauLevel) {
        this.uid       = uid;
        this.name      = name;
        this.email     = email;
        this.phone     = phone;
        this.ndauLevel = ndauLevel;
        this.totalWords = 0; this.totalVoice = 0;
        this.pendingMT = 0; this.earnedMT = 0; this.withdrawnMT = 0;
    }
}

// ══════════════════════════════════════════
//  FICHEIRO 5: models/Translation.java
// ══════════════════════════════════════════
package mz.ndau.digital.models;

import com.google.firebase.Timestamp;
import java.util.List;

public class Translation {
    public String id, userId, userName, sourceLang, sourceText, ndauText, status, type;
    public int wordCount;
    public double earnedMT;
    public List<String> validatedBy, rejectedBy;
    public Timestamp createdAt;

    public Translation() {}
}

// ══════════════════════════════════════════
//  FICHEIRO 6: models/Recording.java
// ══════════════════════════════════════════
package mz.ndau.digital.models;

import com.google.firebase.Timestamp;
import java.util.List;

public class Recording {
    public String id, userId, userName, word, meaning, audioUrl, status, type;
    public double earnedMT;
    public List<String> validatedBy;
    public Timestamp createdAt;

    public Recording() {}
}

// ══════════════════════════════════════════
//  FICHEIRO 7: firebase/FirebaseManager.java
// ══════════════════════════════════════════
package mz.ndau.digital.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.*;
import mz.ndau.digital.models.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FirebaseManager {

    private static FirebaseManager instance;
    private final FirebaseAuth     auth;
    private final FirebaseFirestore db;
    private final FirebaseStorage   storage;

    private FirebaseManager() {
        auth    = FirebaseAuth.getInstance();
        db      = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public static synchronized FirebaseManager get() {
        if (instance == null) instance = new FirebaseManager();
        return instance;
    }

    public FirebaseAuth     auth()    { return auth; }
    public FirebaseFirestore db()     { return db; }
    public FirebaseStorage   storage(){ return storage; }
    public FirebaseUser      current(){ return auth.getCurrentUser(); }

    // ── REGISTO ──────────────────────────────────────────────────
    public void register(String name, String email, String phone,
                         String ndauLevel, String password,
                         OnCompleteCallback<String> cb) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(cred -> {
                String uid = cred.getUser().getUid();
                User user  = new User(uid, name, email, phone, ndauLevel);

                db.collection("users").document(uid)
                  .set(user)
                  .addOnSuccessListener(v -> {
                      // Incrementar total de utilizadores
                      db.collection("platform_stats").document("global")
                        .update("totalUsers", FieldValue.increment(1));
                      cb.onSuccess(uid);
                  })
                  .addOnFailureListener(e -> cb.onError(e.getMessage()));
            })
            .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    // ── LOGIN ─────────────────────────────────────────────────────
    public void login(String email, String password, OnCompleteCallback<String> cb) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(cred -> {
                String uid = cred.getUser().getUid();
                db.collection("users").document(uid)
                  .update("lastActive", FieldValue.serverTimestamp());
                cb.onSuccess(uid);
            })
            .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    // ── PERFIL ────────────────────────────────────────────────────
    public void getUserProfile(String uid, OnCompleteCallback<User> cb) {
        db.collection("users").document(uid)
          .get()
          .addOnSuccessListener(snap -> {
              if (snap.exists()) cb.onSuccess(snap.toObject(User.class));
              else cb.onError("Perfil não encontrado");
          })
          .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    public ListenerRegistration watchProfile(String uid, OnCompleteCallback<User> cb) {
        return db.collection("users").document(uid)
                 .addSnapshotListener((snap, e) -> {
                     if (e != null) { cb.onError(e.getMessage()); return; }
                     if (snap != null && snap.exists()) cb.onSuccess(snap.toObject(User.class));
                 });
    }

    // ── SUBMETER TRADUÇÃO ─────────────────────────────────────────
    public void submitTranslation(String userId, String userName, String sourceLang,
                                  String sourceText, String ndauText,
                                  OnCompleteCallback<String> cb) {
        int wordCount = ndauText.trim().split("\\s+").length;
        double pmt    = Math.floor(wordCount / 10.0) * 5;

        Map<String, Object> data = new HashMap<>();
        data.put("userId",      userId);
        data.put("userName",    userName);
        data.put("sourceLang",  sourceLang);
        data.put("sourceText",  sourceText);
        data.put("ndauText",    ndauText);
        data.put("wordCount",   wordCount);
        data.put("status",      "pending");
        data.put("earnedMT",    0);
        data.put("validatedBy", Arrays.asList());
        data.put("rejectedBy",  Arrays.asList());
        data.put("type",        "text");
        data.put("createdAt",   FieldValue.serverTimestamp());

        db.collection("translations").add(data)
          .addOnSuccessListener(ref -> {
              // Actualizar contagem do utilizador
              db.collection("users").document(userId)
                .update("totalWords",  FieldValue.increment(wordCount),
                        "pendingMT",   FieldValue.increment(pmt));
              cb.onSuccess(ref.getId());
          })
          .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    // ── UPLOAD DE GRAVAÇÃO ────────────────────────────────────────
    public void uploadRecording(String userId, String userName,
                                String word, String meaning,
                                byte[] audioData, OnCompleteCallback<String> cb) {
        StorageReference ref = storage.getReference()
                .child("recordings/" + userId + "/" + System.currentTimeMillis() + "_" + word + ".3gp");

        ref.putBytes(audioData)
           .addOnSuccessListener(snap -> ref.getDownloadUrl()
               .addOnSuccessListener(uri -> {
                   Map<String, Object> data = new HashMap<>();
                   data.put("userId",      userId);
                   data.put("userName",    userName);
                   data.put("word",        word);
                   data.put("meaning",     meaning);
                   data.put("audioUrl",    uri.toString());
                   data.put("status",      "pending");
                   data.put("earnedMT",    0);
                   data.put("validatedBy", Arrays.asList());
                   data.put("type",        "voice");
                   data.put("createdAt",   FieldValue.serverTimestamp());

                   db.collection("recordings").add(data)
                     .addOnSuccessListener(docRef -> {
                         db.collection("users").document(userId)
                           .update("totalVoice", FieldValue.increment(1),
                                   "pendingMT",  FieldValue.increment(1));
                         cb.onSuccess(docRef.getId());
                     })
                     .addOnFailureListener(e -> cb.onError(e.getMessage()));
               }))
           .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    // ── FILA DE VALIDAÇÃO ─────────────────────────────────────────
    public ListenerRegistration watchValidationQueue(String currentUid,
                                                     OnCompleteCallback<java.util.List<Translation>> cb) {
        return db.collection("translations")
                 .whereEqualTo("status", "pending")
                 .orderBy("createdAt")
                 .limit(20)
                 .addSnapshotListener((snap, e) -> {
                     if (e != null) { cb.onError(e.getMessage()); return; }
                     java.util.List<Translation> list = new java.util.ArrayList<>();
                     if (snap != null) {
                         for (DocumentSnapshot d : snap.getDocuments()) {
                             Translation t = d.toObject(Translation.class);
                             if (t != null && !t.userId.equals(currentUid)) {
                                 t.id = d.getId();
                                 list.add(t);
                             }
                         }
                     }
                     cb.onSuccess(list);
                 });
    }

    // ── VALIDAR/REJEITAR TRADUÇÃO ─────────────────────────────────
    public void validateTranslation(String translationId, String validatorId,
                                    boolean approve, OnCompleteCallback<Void> cb) {
        String field = approve ? "validatedBy" : "rejectedBy";
        db.collection("translations").document(translationId)
          .update(field, FieldValue.arrayUnion(validatorId))
          .addOnSuccessListener(v -> cb.onSuccess(null))
          .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    // ── PEDIDO DE LEVANTAMENTO ────────────────────────────────────
    public void requestWithdrawal(String userId, double amount,
                                  String method, String phone,
                                  OnCompleteCallback<String> cb) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId",      userId);
        data.put("amount",      amount);
        data.put("method",      method);
        data.put("phoneNumber", phone);
        data.put("status",      "pending");
        data.put("reference",   "NDW-" + System.currentTimeMillis());
        data.put("createdAt",   FieldValue.serverTimestamp());

        db.collection("withdrawals").add(data)
          .addOnSuccessListener(ref -> {
              db.collection("users").document(userId)
                .update("earnedMT",    FieldValue.increment(-amount),
                        "withdrawnMT", FieldValue.increment(amount));
              cb.onSuccess(ref.getId());
          })
          .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    // ── HISTÓRICO DO UTILIZADOR ────────────────────────────────────
    public void getUserHistory(String userId, OnCompleteCallback<java.util.List<Translation>> cb) {
        db.collection("translations")
          .whereEqualTo("userId", userId)
          .orderBy("createdAt", Query.Direction.DESCENDING)
          .limit(50)
          .get()
          .addOnSuccessListener(snap -> {
              java.util.List<Translation> list = new java.util.ArrayList<>();
              for (DocumentSnapshot d : snap.getDocuments()) {
                  Translation t = d.toObject(Translation.class);
                  if (t != null) { t.id = d.getId(); list.add(t); }
              }
              cb.onSuccess(list);
          })
          .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    // ── INTERFACE CALLBACK ────────────────────────────────────────
    public interface OnCompleteCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }
}

// ══════════════════════════════════════════
//  FICHEIRO 8: ui/SplashActivity.java
// ══════════════════════════════════════════
package mz.ndau.digital.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import mz.ndau.digital.R;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            Intent intent;
            if (auth.getCurrentUser() != null) {
                intent = new Intent(this, MainActivity.class);
            } else {
                intent = new Intent(this, AuthActivity.class);
            }
            startActivity(intent);
            finish();
        }, 2000);
    }
}

// ══════════════════════════════════════════
//  FICHEIRO 9: ui/AuthActivity.java
// ══════════════════════════════════════════
package mz.ndau.digital.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import mz.ndau.digital.R;
import mz.ndau.digital.firebase.FirebaseManager;

public class AuthActivity extends AppCompatActivity {

    private boolean isLogin = true;
    private FirebaseManager fb;

    // Login views
    private EditText etLoginEmail, etLoginPass;
    private Button   btnLogin;

    // Register views
    private EditText etRegName, etRegSurname, etRegEmail, etRegPhone, etRegPass;
    private Spinner  spNdauLevel;
    private Button   btnRegister;

    private LinearLayout llLogin, llRegister;
    private TextView tvSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        fb = FirebaseManager.get();
        bindViews();
        setupListeners();
    }

    private void bindViews() {
        llLogin    = findViewById(R.id.llLogin);
        llRegister = findViewById(R.id.llRegister);
        tvSwitch   = findViewById(R.id.tvSwitch);
        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPass  = findViewById(R.id.etLoginPass);
        btnLogin     = findViewById(R.id.btnLogin);
        etRegName    = findViewById(R.id.etRegName);
        etRegSurname = findViewById(R.id.etRegSurname);
        etRegEmail   = findViewById(R.id.etRegEmail);
        etRegPhone   = findViewById(R.id.etRegPhone);
        etRegPass    = findViewById(R.id.etRegPass);
        spNdauLevel  = findViewById(R.id.spNdauLevel);
        btnRegister  = findViewById(R.id.btnRegister);
    }

    private void setupListeners() {
        tvSwitch.setOnClickListener(v -> {
            isLogin = !isLogin;
            llLogin.setVisibility(isLogin ? View.VISIBLE : View.GONE);
            llRegister.setVisibility(isLogin ? View.GONE : View.VISIBLE);
            tvSwitch.setText(isLogin ? "Não tem conta? Criar conta" : "Já tem conta? Entrar");
        });

        btnLogin.setOnClickListener(v -> doLogin());
        btnRegister.setOnClickListener(v -> doRegister());
    }

    private void doLogin() {
        String email = etLoginEmail.getText().toString().trim();
        String pass  = etLoginPass.getText().toString();
        if (email.isEmpty() || pass.isEmpty()) { toast("Preencha todos os campos"); return; }

        btnLogin.setEnabled(false);
        btnLogin.setText("A entrar...");

        fb.login(email, pass, new FirebaseManager.OnCompleteCallback<String>() {
            @Override public void onSuccess(String uid) {
                startActivity(new Intent(AuthActivity.this, MainActivity.class));
                finish();
            }
            @Override public void onError(String err) {
                toast("Erro: " + err);
                btnLogin.setEnabled(true);
                btnLogin.setText("Entrar");
            }
        });
    }

    private void doRegister() {
        String name     = etRegName.getText().toString().trim();
        String surname  = etRegSurname.getText().toString().trim();
        String email    = etRegEmail.getText().toString().trim();
        String phone    = etRegPhone.getText().toString().trim();
        String pass     = etRegPass.getText().toString();
        String level    = spNdauLevel.getSelectedItem().toString().toLowerCase();

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) { toast("Preencha os campos obrigatórios"); return; }

        btnRegister.setEnabled(false);
        btnRegister.setText("A criar conta...");

        fb.register(name + " " + surname, email, phone, level, pass,
            new FirebaseManager.OnCompleteCallback<String>() {
                @Override public void onSuccess(String uid) {
                    toast("Conta criada! Bem-vindo(a) " + name);
                    startActivity(new Intent(AuthActivity.this, MainActivity.class));
                    finish();
                }
                @Override public void onError(String err) {
                    toast("Erro: " + err);
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Criar Conta");
                }
            });
    }

    private void toast(String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
    }
}

// ══════════════════════════════════════════
//  FICHEIRO 10: ui/MainActivity.java
// ══════════════════════════════════════════
package mz.ndau.digital.ui;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.ListenerRegistration;
import mz.ndau.digital.R;
import mz.ndau.digital.firebase.FirebaseManager;
import mz.ndau.digital.models.User;
import mz.ndau.digital.ui.fragments.*;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private ListenerRegistration profileListener;
    public static User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNav);
        loadUserProfile();
        setupBottomNav();

        // Carregar fragmento inicial
        loadFragment(new HomeFragment());
    }

    private void loadUserProfile() {
        String uid = FirebaseManager.get().current().getUid();
        profileListener = FirebaseManager.get().watchProfile(uid,
            new FirebaseManager.OnCompleteCallback<User>() {
                @Override public void onSuccess(User user) {
                    currentUser = user;
                    currentUser.uid = uid;
                    // Notificar fragmentos activos
                    Fragment f = getSupportFragmentManager().findFragmentById(R.id.navHostFragment);
                    if (f instanceof ProfileAwareFragment) ((ProfileAwareFragment) f).onProfileUpdated(user);
                }
                @Override public void onError(String err) {}
            });
    }

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if      (id == R.id.navHome)      loadFragment(new HomeFragment());
            else if (id == R.id.navTranslate) loadFragment(new TranslateFragment());
            else if (id == R.id.navVoice)     loadFragment(new VoiceFragment());
            else if (id == R.id.navValidate)  loadFragment(new ValidateFragment());
            else if (id == R.id.navProfile)   loadFragment(new ProfileFragment());
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.navHostFragment, fragment)
            .commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (profileListener != null) profileListener.remove();
    }

    public interface ProfileAwareFragment {
        void onProfileUpdated(User user);
    }
}

// ══════════════════════════════════════════
//  FICHEIRO 11: ui/fragments/TranslateFragment.java
// ══════════════════════════════════════════
package mz.ndau.digital.ui.fragments;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import mz.ndau.digital.R;
import mz.ndau.digital.firebase.FirebaseManager;
import mz.ndau.digital.ui.MainActivity;

public class TranslateFragment extends Fragment {

    private EditText etSource, etNdau;
    private Spinner  spLang;
    private Button   btnSubmit;
    private TextView tvWordCount;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
        View v = inflater.inflate(R.layout.fragment_translate, container, false);

        etSource    = v.findViewById(R.id.etSource);
        etNdau      = v.findViewById(R.id.etNdau);
        spLang      = v.findViewById(R.id.spLang);
        btnSubmit   = v.findViewById(R.id.btnSubmit);
        tvWordCount = v.findViewById(R.id.tvWordCount);

        // Contador de palavras em tempo real
        etNdau.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void afterTextChanged(android.text.Editable s) {
                int c = s.toString().trim().isEmpty() ? 0 : s.toString().trim().split("\\s+").length;
                tvWordCount.setText(c + " palavras");
                // Mostrar estimativa de pagamento
                if (c >= 10) {
                    int mt = (int)(Math.floor(c / 10.0) * 5);
                    tvWordCount.setText(c + " palavras (+"+mt+" MT pendente)");
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        btnSubmit.setOnClickListener(view -> submitTranslation());
        return v;
    }

    private void submitTranslation() {
        String srcLang = spLang.getSelectedItem().toString().substring(0, 2).toLowerCase();
        String src     = etSource.getText().toString().trim();
        String ndau    = etNdau.getText().toString().trim();

        if (src.isEmpty() || ndau.isEmpty()) {
            Toast.makeText(getContext(), "Preencha ambas as caixas", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("A enviar...");

        String uid  = MainActivity.currentUser.uid;
        String name = MainActivity.currentUser.name;

        FirebaseManager.get().submitTranslation(uid, name, srcLang, src, ndau,
            new FirebaseManager.OnCompleteCallback<String>() {
                @Override public void onSuccess(String id) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "✅ Tradução submetida ao Firebase!", Toast.LENGTH_LONG).show();
                        etSource.setText("");
                        etNdau.setText("");
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Submeter Tradução");
                    });
                }
                @Override public void onError(String err) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Erro: " + err, Toast.LENGTH_LONG).show();
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Submeter Tradução");
                    });
                }
            });
    }
}

// ══════════════════════════════════════════
//  FICHEIRO 12: ui/fragments/VoiceFragment.java
// ══════════════════════════════════════════
package mz.ndau.digital.ui.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.io.*;
import mz.ndau.digital.R;
import mz.ndau.digital.firebase.FirebaseManager;
import mz.ndau.digital.ui.MainActivity;

public class VoiceFragment extends Fragment {

    private static final int REQUEST_AUDIO = 1001;
    private EditText etWord, etMeaning;
    private Button   btnRecord, btnSend;
    private TextView tvStatus;
    private MediaRecorder recorder;
    private File   audioFile;
    private boolean isRecording = false;

    @Override
    public View onCreateView(LayoutInflater i, ViewGroup c, Bundle b) {
        View v = i.inflate(R.layout.fragment_voice, c, false);
        etWord    = v.findViewById(R.id.etVoiceWord);
        etMeaning = v.findViewById(R.id.etVoiceMeaning);
        btnRecord = v.findViewById(R.id.btnRecord);
        btnSend   = v.findViewById(R.id.btnSendVoice);
        tvStatus  = v.findViewById(R.id.tvRecStatus);
        btnSend.setVisibility(View.GONE);
        btnRecord.setOnClickListener(x -> toggleRecording());
        btnSend.setOnClickListener(x -> uploadRecording());
        return v;
    }

    private void toggleRecording() {
        if (!checkAudioPermission()) return;
        if (!isRecording) startRecording();
        else stopRecording();
    }

    private boolean checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO);
            return false;
        }
        return true;
    }

    private void startRecording() {
        String word = etWord.getText().toString().trim();
        if (word.isEmpty()) { Toast.makeText(getContext(), "Escreva a palavra Ndau", Toast.LENGTH_SHORT).show(); return; }

        try {
            audioFile = new File(requireContext().getCacheDir(), "ndau_" + System.currentTimeMillis() + ".3gp");
            recorder  = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(audioFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();
            isRecording = true;
            btnRecord.setText("⏹ Parar");
            tvStatus.setText("🔴 A gravar...");
        } catch (IOException e) {
            Toast.makeText(getContext(), "Erro ao gravar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopRecording() {
        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
        }
        isRecording = false;
        btnRecord.setText("🎤 Gravar");
        tvStatus.setText("✅ Gravação pronta");
        btnSend.setVisibility(View.VISIBLE);
    }

    private void uploadRecording() {
        if (audioFile == null || !audioFile.exists()) return;
        String word    = etWord.getText().toString().trim();
        String meaning = etMeaning.getText().toString().trim();

        btnSend.setEnabled(false);
        btnSend.setText("A enviar...");

        // Ler ficheiro como bytes
        try {
            byte[] audioData = new byte[(int) audioFile.length()];
            FileInputStream fis = new FileInputStream(audioFile);
            fis.read(audioData); fis.close();

            String uid  = MainActivity.currentUser.uid;
            String name = MainActivity.currentUser.name;

            FirebaseManager.get().uploadRecording(uid, name, word, meaning, audioData,
                new FirebaseManager.OnCompleteCallback<String>() {
                    @Override public void onSuccess(String id) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "🎙️ Enviado para Firebase! \""+word+"\"", Toast.LENGTH_LONG).show();
                            etWord.setText(""); etMeaning.setText("");
                            tvStatus.setText("Pronto para gravar");
                            btnSend.setVisibility(View.GONE);
                            btnSend.setEnabled(true);
                            btnSend.setText("☁️ Enviar");
                        });
                    }
                    @Override public void onError(String err) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Erro: "+err, Toast.LENGTH_LONG).show();
                            btnSend.setEnabled(true);
                            btnSend.setText("☁️ Enviar");
                        });
                    }
                });
        } catch (IOException e) {
            Toast.makeText(getContext(), "Erro ao ler ficheiro", Toast.LENGTH_LONG).show();
        }
    }
}

// ══════════════════════════════════════════
//  FICHEIRO 13: res/values/strings.xml
// ══════════════════════════════════════════
/*
<resources>
    <string name="app_name">Ndau Digital</string>
    <string name="tagline">Preservar e Digitalizar a Língua Ndau</string>
    <string name="nav_home">Início</string>
    <string name="nav_translate">Traduzir</string>
    <string name="nav_voice">Voz</string>
    <string name="nav_validate">Validar</string>
    <string name="nav_profile">Perfil</string>
    <string name="btn_login">Entrar</string>
    <string name="btn_register">Criar Conta</string>
    <string name="btn_submit">Submeter Tradução</string>
    <string name="btn_record">🎤 Gravar</string>
    <string name="btn_send_voice">☁️ Enviar para Firebase</string>
    <string name="label_source">Texto Original</string>
    <string name="label_ndau">Tradução em Ndau</string>
    <string name="hint_source">Escreva o texto a traduzir...</string>
    <string name="hint_ndau">Escreva a tradução em Ndau...</string>
    <string name="hint_word">Palavra Ndau (ex: Musha)</string>
    <string name="hint_meaning">Significado em Português</string>
    <string-array name="ndau_levels">
        <item>Falante Nativo</item>
        <item>Fluente</item>
        <item>Intermédio</item>
        <item>Aprendiz</item>
    </string-array>
    <string-array name="source_languages">
        <item>Português</item>
        <item>Inglês</item>
        <item>Shona</item>
    </string-array>
</resources>
*/

// ══════════════════════════════════════════
//  FICHEIRO 14: res/values/colors.xml
// ══════════════════════════════════════════
/*
<resources>
    <color name="ndau_bg">#0D0D0B</color>
    <color name="ndau_surface">#161612</color>
    <color name="ndau_gold">#C8A84B</color>
    <color name="ndau_gold_light">#E8C86B</color>
    <color name="ndau_earth">#8B5E3C</color>
    <color name="ndau_green">#4A7C59</color>
    <color name="ndau_green_light">#6AB87A</color>
    <color name="ndau_text">#F0EAD8</color>
    <color name="ndau_muted">#7A7460</color>
    <color name="ndau_accent">#D4824A</color>
    <color name="ndau_danger">#C05A3A</color>
    <color name="ndau_border">#2A2A22</color>
    <color name="white">#FFFFFF</color>
    <color name="black">#000000</color>
</resources>
*/

// ══════════════════════════════════════════
//  FICHEIRO 15: res/values/themes.xml
// ══════════════════════════════════════════
/*
<resources>
    <style name="Theme.NdauDigital" parent="Theme.MaterialComponents.DayNight.NoActionBar">
        <item name="colorPrimary">@color/ndau_gold</item>
        <item name="colorPrimaryVariant">@color/ndau_earth</item>
        <item name="colorOnPrimary">@color/ndau_bg</item>
        <item name="colorSecondary">@color/ndau_green</item>
        <item name="colorBackground">@color/ndau_bg</item>
        <item name="android:colorBackground">@color/ndau_bg</item>
        <item name="android:windowBackground">@color/ndau_bg</item>
        <item name="android:textColor">@color/ndau_text</item>
        <item name="android:fontFamily">@font/dm_sans</item>
    </style>

    <style name="Theme.NdauDigital.Splash" parent="Theme.NdauDigital">
        <item name="android:windowFullscreen">true</item>
        <item name="android:windowBackground">@color/ndau_bg</item>
    </style>

    <!-- Botão principal dourado -->
    <style name="NdauButton" parent="Widget.MaterialComponents.Button">
        <item name="backgroundTint">@color/ndau_gold</item>
        <item name="android:textColor">@color/ndau_bg</item>
        <item name="android:textStyle">bold</item>
        <item name="cornerRadius">10dp</item>
    </style>

    <!-- Botão outline -->
    <style name="NdauButtonOutline" parent="Widget.MaterialComponents.Button.OutlinedButton">
        <item name="strokeColor">@color/ndau_border</item>
        <item name="android:textColor">@color/ndau_text</item>
        <item name="cornerRadius">10dp</item>
    </style>

    <!-- Card surface -->
    <style name="NdauCard" parent="Widget.MaterialComponents.CardView">
        <item name="cardBackgroundColor">@color/ndau_surface</item>
        <item name="strokeColor">@color/ndau_border</item>
        <item name="strokeWidth">1dp</item>
        <item name="cardCornerRadius">12dp</item>
        <item name="cardElevation">0dp</item>
    </style>
</resources>
*/

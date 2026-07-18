# 🌍 NDAU DIGITAL — GUIA DE CONFIGURAÇÃO COMPLETO

## ARQUITECTURA DO PROJECTO

```
ndau-digital/
├── 🌐 WEB (HTML + Firebase JS SDK)
│   ├── ndau-web-firebase.html    ← Site principal com Firebase integrado
│   └── firebase-config.js        ← Funções Firebase reutilizáveis
│
├── 🤖 ANDROID (Java + Firebase SDK)
│   └── app/src/main/java/mz/ndau/digital/
│       ├── NdauApp.java
│       ├── firebase/FirebaseManager.java    ← TODA lógica Firebase
│       ├── models/User.java
│       ├── models/Translation.java
│       ├── models/Recording.java
│       └── ui/
│           ├── SplashActivity.java
│           ├── AuthActivity.java
│           ├── MainActivity.java
│           └── fragments/
│               ├── HomeFragment.java
│               ├── TranslateFragment.java
│               ├── VoiceFragment.java
│               ├── ValidateFragment.java
│               └── ProfileFragment.java
│
└── 🔥 FIREBASE
    ├── firestore.rules              ← Regras de segurança
    └── firestore-schema.md          ← Estrutura da BD
```

---

## PASSO 1 — CRIAR PROJECTO FIREBASE

1. Acesse: https://console.firebase.google.com
2. Clique **"Criar projecto"**
3. Nome: `ndau-digital`
4. Active o **Google Analytics**
5. Confirme

### Activar Serviços:
- **Authentication** → Email/Password → Activar
- **Firestore Database** → Criar base de dados → Modo produção
- **Storage** → Começar → Modo produção
- **Analytics** → Activo por padrão

---

## PASSO 2 — CONFIGURAR O SITE (Web)

### Obter as credenciais:
1. Firebase Console → Configurações do projecto ⚙️
2. **"Adicionar app"** → Web `</>`
3. Nome: `ndau-digital-web`
4. Copie o `firebaseConfig`

### Actualizar o ficheiro:
Abra `ndau-web-firebase.html` e substitua:
```javascript
const firebaseConfig = {
  apiKey:            "SUA_API_KEY",        // ← substituir
  authDomain:        "ndau-digital.firebaseapp.com",
  projectId:         "ndau-digital",
  storageBucket:     "ndau-digital.appspot.com",
  messagingSenderId: "SEU_SENDER_ID",      // ← substituir
  appId:             "SEU_APP_ID"          // ← substituir
};
```

### Publicar o site (grátis):
```bash
npm install -g firebase-tools
firebase login
firebase init hosting
# Pasta pública: . (raiz)
# SPA: não
firebase deploy
# URL: https://ndau-digital.web.app
```

---

## PASSO 3 — CONFIGURAR APP ANDROID

### Registar a app no Firebase:
1. Firebase Console → Adicionar app → Android 🤖
2. Package name: `mz.ndau.digital`
3. Descarregue o ficheiro **`google-services.json`**
4. Coloque em: `app/google-services.json`

### Estrutura de pastas Android Studio:
```
Novo Projecto → Empty Activity
Package:     mz.ndau.digital
Language:    Java
Min SDK:     API 24 (Android 7.0)
```

### Copiar os ficheiros Java:
Copie cada classe do ficheiro `NdauAndroid.java` para a pasta correspondente:

| Classe | Pasta |
|--------|-------|
| `NdauApp.java` | `app/src/main/java/mz/ndau/digital/` |
| `FirebaseManager.java` | `.../firebase/` |
| `User.java`, `Translation.java`, `Recording.java` | `.../models/` |
| `SplashActivity.java`, `AuthActivity.java`, `MainActivity.java` | `.../ui/` |
| `TranslateFragment.java`, `VoiceFragment.java` | `.../ui/fragments/` |

### `build.gradle` (nível app):
```groovy
plugins {
    id 'com.android.application'
    id 'com.google.gms.google-services'
}
dependencies {
    implementation platform('com.google.firebase:firebase-bom:32.7.0')
    implementation 'com.google.firebase:firebase-auth'
    implementation 'com.google.firebase:firebase-firestore'
    implementation 'com.google.firebase:firebase-storage'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
}
```

### `build.gradle` (nível projecto):
```groovy
plugins {
    id 'com.google.gms.google-services' version '4.4.0' apply false
}
```

---

## PASSO 4 — CONFIGURAR REGRAS FIRESTORE

No Firebase Console → Firestore → **Regras**, cole o conteúdo de `firestore.rules`.

---

## PASSO 5 — CRIAR DOCUMENTO DE STATS INICIAIS

No Firebase Console → Firestore → **Adicionar documento**:
- Colecção: `platform_stats`
- ID: `global`
- Campos:
  ```
  totalWords:  0  (number)
  totalVoice:  0  (number)
  totalUsers:  0  (number)
  totalPaidMT: 0  (number)
  ```

---

## ESTRUTURA DA BASE DE DADOS FIRESTORE

### `/users/{uid}`
| Campo | Tipo | Descrição |
|-------|------|-----------|
| `name` | string | Nome completo |
| `email` | string | Email |
| `phone` | string | Número M-Pesa/E-Mola |
| `ndauLevel` | string | native/fluent/intermediate/learner |
| `totalWords` | number | Total palavras traduzidas |
| `totalVoice` | number | Total gravações |
| `pendingMT` | number | MT aguardando validação |
| `earnedMT` | number | MT disponível para levantar |
| `withdrawnMT` | number | MT já levantados |

### `/translations/{id}`
| Campo | Tipo | Descrição |
|-------|------|-----------|
| `userId` | string | UID do tradutor |
| `sourceLang` | string | pt/en/sn |
| `sourceText` | string | Texto original |
| `ndauText` | string | Tradução Ndau |
| `wordCount` | number | Nº de palavras |
| `status` | string | pending/validated/rejected |
| `earnedMT` | number | MT ganhos (após validação) |
| `validatedBy` | array | UIDs dos validadores |

### `/recordings/{id}`
| Campo | Tipo | Descrição |
|-------|------|-----------|
| `word` | string | Palavra Ndau gravada |
| `meaning` | string | Significado em Português |
| `audioUrl` | string | URL do Firebase Storage |
| `status` | string | pending/validated/rejected |

### `/withdrawals/{id}`
| Campo | Tipo | Descrição |
|-------|------|-----------|
| `amount` | number | Valor em MT |
| `method` | string | mpesa/emola/bank |
| `phoneNumber` | string | Número de destino |
| `status` | string | pending/processing/completed |
| `reference` | string | NDW-TIMESTAMP |

---

## SISTEMA DE PAGAMENTO — LÓGICA

```
TEXTO:
  Utilizador submete → status: "pending"
  2 utilizadores validam → status: "validated"
  earnedMT += floor(wordCount / 10) * 5
  pendingMT -= valor; earnedMT += valor

VOZ:
  Utilizador grava → status: "pending"
  2 utilizadores validam → status: "validated"
  earnedMT += 1 MT por gravação (10 MT a cada 10)

LEVANTAMENTO:
  Utilizador pede levante → /withdrawals criado
  Admin processa via M-Pesa API / E-Mola API / banco
  status atualizado para "completed"
```

---

## PASSOS FUTUROS

### Integração M-Pesa (Moçambique)
```
API: https://developer.mpesa.vm.co.mz
→ Registar empresa
→ Obter API Key + Secret
→ Endpoint: POST /c2b/transactions (pagar utilizador)
→ Integrar nas Firebase Cloud Functions
```

### Firebase Cloud Functions (pagamentos automáticos)
```javascript
// functions/index.js
exports.processWithdrawal = onDocumentCreated(
  'withdrawals/{id}', async (event) => {
    const data = event.data.data();
    if (data.method === 'mpesa') {
      await mpesaAPI.sendMoney(data.phoneNumber, data.amount);
      await event.data.ref.update({ status: 'completed' });
    }
  }
);
```

### Submeter dados ao Google Translate
```
→ Google Cloud Translation API
→ Criar par de idiomas personalizado (pt-ndau)
→ Exportar dados validados em formato TMX
→ Submeter ao programa Common Voice (Mozilla)
   para os dados de voz
```

---

## RECURSOS ÚTEIS

| Recurso | URL |
|---------|-----|
| Firebase Console | https://console.firebase.google.com |
| Firebase Docs | https://firebase.google.com/docs |
| Android Studio | https://developer.android.com/studio |
| Google Translate API | https://cloud.google.com/translate |
| Mozilla Common Voice | https://commonvoice.mozilla.org |
| M-Pesa API MZ | https://developer.mpesa.vm.co.mz |

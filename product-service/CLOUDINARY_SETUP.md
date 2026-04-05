# Cloudinary Kalıcı Görsel Kurulumu

Bu servis artık iki modda çalışır:
- `UPLOAD_PROVIDER=cloudinary`: sadece Cloudinary kullanır (önerilen production).
- `UPLOAD_PROVIDER=auto`: Cloudinary bilgileri varsa Cloudinary, yoksa local `/uploads`.

## Gerekli ENV değişkenleri

Aşağıdaki değerleri backend çalıştığı ortama ekleyin:

- `UPLOAD_PROVIDER=cloudinary`
- `CLOUDINARY_CLOUD_NAME=...`
- `CLOUDINARY_API_KEY=...`
- `CLOUDINARY_API_SECRET=...`
- `CLOUDINARY_FOLDER=serravit` (opsiyonel, varsayılan `serravit`)

## Cloudinary panelinden alınacak bilgiler

Cloudinary Dashboard > API Keys bölümünden:
- Cloud name
- API Key
- API Secret

## Önemli notlar

- `UPLOAD_PROVIDER=auto` modunda credentials yoksa servis local diske düşer.
  Deploy sonrası disk sıfırlanabilen ortamlarda (ör. ephemeral storage) görseller kaybolur.
- Kalıcı kullanım için production ortamında `UPLOAD_PROVIDER=cloudinary` kullanın.
- Eski local URL'ler (`/uploads/...`) veritabanında kaldıysa yeniden yükleme veya migration gerekir.

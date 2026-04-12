# Backup & Disaster Recovery Kiti (Coolify / Docker)

Bu klasör, canlı ortam için **düzenli yedek + geri dönüş** akışını hazırlar.

İçerik:
- `backup.sh`: DB + volume + opsiyonel ekstra dizin yedeği alır
- `restore.sh`: yedekten DB/volume geri yükler
- `verify-backup.sh`: checksum + gzip + geçici DB restore testi yapar
- `backup.conf.example`: konfig şablonu

## 1) İlk kurulum

```bash
cd /path/to/ecommerce-platform
cp ops/backup/backup.conf.example ops/backup/backup.conf
chmod +x ops/backup/*.sh
```

`ops/backup/backup.conf` içinde en az şunları doldur:
- `BACKUP_ROOT`
- `PG_CONTAINER`
- `PG_USER`
- `PG_DB`
- `DOCKER_VOLUMES`

## 2) Manuel backup alma

```bash
./ops/backup/backup.sh ./ops/backup/backup.conf
```

Başarılı olunca çıktı sonunda hedef dizini verir:
`/var/backups/ecommerce-platform/YYYY/MM/DD/backup_<host>_<timestamp>`

## 3) Yedeği doğrulama (zorunlu öneri)

```bash
./ops/backup/verify-backup.sh /var/backups/ecommerce-platform/2026/04/12/backup_srv_2026-04-12T02-00-00
```

Bu test:
- checksum kontrolü
- gzip bütünlüğü
- geçici postgres container içinde restore testi

## 4) Geri yükleme (disaster)

Önemli:
- Önce backend container’ını durdur
- Geri yüklemeden önce son snapshot al

Sadece DB:
```bash
./ops/backup/restore.sh <backup_dir> --db --yes
```

Sadece volume:
```bash
./ops/backup/restore.sh <backup_dir> --volumes --yes
```

DB + volume:
```bash
./ops/backup/restore.sh <backup_dir> --db --volumes --yes
```

## 5) Cron ile otomatikleştirme

`crontab -e` içine:

```cron
# Her gün 02:00 full backup
0 2 * * * cd /path/to/ecommerce-platform && ./ops/backup/backup.sh ./ops/backup/backup.conf >> /var/log/ecommerce-backup.log 2>&1

# Her pazar 03:30 backup verify
30 3 * * 0 cd /path/to/ecommerce-platform && LATEST=$(find /var/backups/ecommerce-platform -type d -name "backup_*" | sort | tail -n 1) && ./ops/backup/verify-backup.sh "$LATEST" >> /var/log/ecommerce-backup-verify.log 2>&1
```

## 6) Offsite (farklı sağlayıcı) önerilen

`backup.conf` içinde:
- `ENABLE_RCLONE=1`
- `RCLONE_REMOTE=wasabi-serravit:backup/ecommerce-platform`

Sunucuda tek seferlik:
```bash
rclone config
```

## 7) Önerilen politika (production)

- DB backup: günlük + en az 30 gün retention
- Her hafta restore test (verify script)
- Offsite yedek: farklı sağlayıcı (S3/Wasabi/Backblaze)
- Sunucu snapshot: haftalık
- `.env` / secrets: ayrıca şifreli kasada (1Password/Vault)

## 8) Hedef metrik

- RPO: 6 saat
- RTO: 1 saat

Bu kit RPO/RTO’yu tek başına garanti etmez; düzenli restore testi ve operasyon disiplini gerekir.

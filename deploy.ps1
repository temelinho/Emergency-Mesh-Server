# Sunucu Bilgileri
$ServerIP = "37.247.101.86"
$User = "root"

Write-Host ">>> Sunucuya dosyalar gonderiliyor..." -ForegroundColor Cyan

# Onemli dosyalari ziplayip gonderelim (target ve node_modules haric)
tar -czf deployment.tar.gz src pom.xml Dockerfile docker-compose.yml schema.sql

# SCP ile sunucuya gonder
Write-Host ">>> Lutfen sunucu sifresini girin:" -ForegroundColor Yellow
scp deployment.tar.gz ${User}@${ServerIP}:/root/

# Sunucuda ac ve docker'i baslat
Write-Host ">>> Sunucuda kurulum baslatiliyor..." -ForegroundColor Cyan
ssh ${User}@${ServerIP} "cd /root && tar -xzf deployment.tar.gz && docker-compose up -d --build && rm deployment.tar.gz"

Write-Host ">>> Islem tamamlandi!" -ForegroundColor Green

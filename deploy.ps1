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
ssh ${User}@${ServerIP} "cd /root && tar -xzf deployment.tar.gz && curl -SL https://github.com/docker/compose/releases/download/v2.27.0/docker-compose-linux-x86_64 -o /usr/bin/docker-compose && chmod +x /usr/bin/docker-compose && docker-compose up -d --build && rm deployment.tar.gz"

Write-Host ">>> Islem tamamlandi!" -ForegroundColor Green

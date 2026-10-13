#!/usr/bin/env bash
# Gera os pacotes prontos para entregar, com o Java embutido (o cliente não precisa instalar nada):
#   dist/Almoxarifado-<versão>-windows-x64.zip   -> descompactar e abrir Almoxarifado.exe
#   dist/Almoxarifado-<versão>-linux-x64.tar.gz  -> descompactar e rodar Almoxarifado/bin/Almoxarifado
#   dist/almoxarifado-<versão>-1.x86_64.rpm      -> instalador para Fedora/RHEL (se houver rpmbuild)
#
# Requisitos na máquina de build (Linux): JDK 25 com jpackage, curl, unzip, zip e tar.
set -euo pipefail

cd "$(dirname "$0")/.."

VERSION=1.1.0
JAVA_FEATURE=25
CACHE=packaging/.cache
DIST=dist

# Baixa (uma vez) o JRE da Eclipse Temurin para o sistema pedido e confere o SHA-256.
fetch_jre() {
    local os=$1
    local url file
    # A API responde com um redirecionamento para o arquivo da release no GitHub.
    url=$(curl -fsS -o /dev/null -w '%{redirect_url}' \
        "https://api.adoptium.net/v3/binary/latest/${JAVA_FEATURE}/ga/${os}/x64/jre/hotspot/normal/eclipse")
    if [ -z "$url" ]; then
        echo "Não foi possível localizar o JRE ${JAVA_FEATURE} para ${os}." >&2
        return 1
    fi
    file="$CACHE/$(basename "${url//%2B/+}")"
    mkdir -p "$CACHE"
    if [ ! -f "$file" ]; then
        echo ">> Baixando JRE para ${os}..." >&2
        curl -fSL --progress-bar -o "$file.part" "$url"
        local expected
        expected=$(curl -fsSL "$url.sha256.txt" | cut -d' ' -f1)
        echo "$expected  $file.part" | sha256sum -c --quiet - >&2
        mv "$file.part" "$file"
    fi
    echo "$file"
}

echo ">> Compilando e testando..."
./mvnw -q -Pwindows clean package

rm -rf "$DIST"
mkdir -p "$DIST/stage"
cp target/almoxarifado.jar "$DIST/stage/"

echo ">> Montando pacote Windows..."
WIN="$DIST/windows/Almoxarifado"
mkdir -p "$WIN/app" "$DIST/windows/jre"
WINDOWS_JRE=$(fetch_jre windows)
unzip -q "$WINDOWS_JRE" -d "$DIST/windows/jre"
mv "$DIST"/windows/jre/* "$WIN/runtime"
rmdir "$DIST/windows/jre"
cp target/Almoxarifado.exe "$WIN/"
cp target/almoxarifado.jar "$WIN/app/"
(cd "$DIST/windows" && zip -qr "../Almoxarifado-$VERSION-windows-x64.zip" Almoxarifado)

echo ">> Montando pacote Linux..."
mkdir -p "$DIST/linux/jre"
LINUX_JRE=$(fetch_jre linux)
tar -xzf "$LINUX_JRE" -C "$DIST/linux/jre" --strip-components=1
jpackage --type app-image \
    --name Almoxarifado \
    --app-version "$VERSION" \
    --vendor "Simplifica Tech" \
    --description "Almoxarifado - Controle de Estoque" \
    --input "$DIST/stage" \
    --main-jar almoxarifado.jar \
    --main-class com.almoxarifado.MainLocal \
    --java-options "--enable-native-access=ALL-UNNAMED" \
    --runtime-image "$DIST/linux/jre" \
    --icon packaging/almoxarifado.png \
    --dest "$DIST/linux"
rm -rf "$DIST/linux/jre"
tar -czf "$DIST/Almoxarifado-$VERSION-linux-x64.tar.gz" -C "$DIST/linux" Almoxarifado

if command -v rpmbuild >/dev/null; then
    echo ">> Gerando instalador .rpm..."
    jpackage --type rpm \
        --name Almoxarifado \
        --linux-package-name almoxarifado \
        --app-version "$VERSION" \
        --vendor "Simplifica Tech" \
        --description "Almoxarifado - Controle de Estoque" \
        --app-image "$DIST/linux/Almoxarifado" \
        --icon packaging/almoxarifado.png \
        --linux-shortcut \
        --linux-menu-group "Office" \
        --dest "$DIST"
fi

rm -rf "$DIST/stage"
echo
echo "Pronto:"
ls -1 "$DIST"/*.zip "$DIST"/*.tar.gz "$DIST"/*.rpm 2>/dev/null

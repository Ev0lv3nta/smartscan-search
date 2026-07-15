# Происхождение SmartScan Android Library

Каталог `sdk` содержит локально импортированные части [smartscanapp/smartscan-android-lib](https://github.com/smartscanapp/smartscan-android-lib).

## Зафиксированный источник

- repository: `https://github.com/smartscanapp/smartscan-android-lib.git`;
- revision: [`8f40e7ff155ad7934cd72022eae21513b33b33f4`](https://github.com/smartscanapp/smartscan-android-lib/commit/8f40e7ff155ad7934cd72022eae21513b33b33f4);
- дата revision: 30 июня 2026 года;
- лицензия: GNU General Public License v3.0, копия находится в [LICENSE](LICENSE).

Импортированы исходники и тесты модулей `core` и `ml`, их native sources и ProGuard rules. Отдельная Gradle-обвязка, publishing configuration, wrapper, `local.properties`, build outputs и документация исходного SDK не копировались.

Build scripts адаптированы к единому Android Gradle Plugin, Kotlin, KSP и version catalog этого monorepo. Package names `com.fpf.smartscansdk.*` сохранены, чтобы не скрывать происхождение и не менять API приложения в механическом PR импорта.

## Обновление

SDK не обновляется автоматическим копированием ветки `main`. Каждое обновление выполняется отдельным pull request, который указывает старую и новую revisions, перечисляет перенесённые upstream commits, объясняет локальные конфликты и проходит полный CI приложения и SDK.

Локальные изменения после импорта принадлежат истории SmartScan Search и распространяются вместе с производной работой под GPLv3.

## Сторонний native-код

Модуль `core` включает исходники `nmslib/hnswlib`, добавленные в SmartScan SDK commit `d74acf9a377daa08a12851dbc76de85345bc5909`. Все семь vendored headers побайтово совпадают с upstream hnswlib revision `d9b3608c83d83b46c96e25088cb1d729b29dcfe9` (release 0.9.0). Hnswlib распространяется под Apache License 2.0; отдельные provenance и текст лицензии находятся в [`third_party/hnswlib`](third_party/hnswlib).

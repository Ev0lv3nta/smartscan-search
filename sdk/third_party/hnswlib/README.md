# hnswlib provenance

Исходники в `sdk/core/src/main/cpp/hnswlib` происходят из [nmslib/hnswlib](https://github.com/nmslib/hnswlib).

- upstream revision: [`d9b3608c83d83b46c96e25088cb1d729b29dcfe9`](https://github.com/nmslib/hnswlib/commit/d9b3608c83d83b46c96e25088cb1d729b29dcfe9);
- release line: 0.9.0;
- лицензия: Apache License 2.0;
- импорт в SmartScan SDK: commit [`d74acf9a377daa08a12851dbc76de85345bc5909`](https://github.com/smartscanapp/smartscan-android-lib/commit/d74acf9a377daa08a12851dbc76de85345bc5909).

Побайтовое сравнение подтвердило совпадение файлов `bruteforce.h`, `hnswalg.h`, `hnswlib.h`, `space_ip.h`, `space_l2.h`, `stop_condition.h` и `visited_list_pool.h` с указанной upstream revision. JNI bridge и Android build configuration относятся к SmartScan SDK, а не к hnswlib upstream.

Архитектура SmartScan Search Core v2 не использует HNSW для целевого индекса. Этот код временно сохраняется для эквивалентности импортированного legacy SDK и должен быть удалён вместе с legacy HNSW path после перехода приложения на exact flat SIMD scanner.

# cache sample

## overview

spring cacheの caffeine 実装でいろいろ試した。

使い方

```PS
PS> ./mvnw spring-boot:build-image
PS> docker-compose up -d
```

browserで http://localhost:3000 を開く.  
admin/adminでログイン後、datasourceをprometheusに設定.  
URLを http://prometheus:9090 にして保存

http://localhost:8080/{key} にアクセス。  
※ {key} 毎にキャッシュをするように実装。アクセスしてみて試そう。

### 実装面

とりあえず、何も考えず `@Cachable` でうまいことやってくれそう感半端なし。

キャッシュの有効期限の設定方法が複数ある。

* expireAfterAccessを設定すると、メソッドが最後に呼ばれた時から設定した時間でキャッシュが消える
* expireAfterWriteを設定すると、キャッシュが最初に作られた時から設定した時間でキャッシュが消える

メモリに乗せる量のコントロールはどうやったらよいのでしょう？  
性能試験で頑張る？

### 可視化回り

やったこと

* `application.yml` で `spring.cache.caffeine.spec` に `recordStats` を設定する必要あり。
* 同様に、キャッシュ名も設定する必要あり？ ~~（よくわかっていない）~~
    * デフォルトでは、キャッシュ名を静的に記載しないとmicrometerに登録されないらしい。  
      自力でmicrometerに動的登録をしている人を見つけたので真似てみた。
* caffeineがmicrometerに対応しているから、prometheusで収集可能。
* 収集した結果の可視化を試してみたダッシュボードを `./dev/grafana/test-dashborad.json` にエクスポート

キャッシュヒット数とヒットしなかった数が出るので、その数値からキャッシュヒット率が出せる。  
キャッシュ数も出せるので、その推移も見れる。

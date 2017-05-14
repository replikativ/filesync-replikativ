# filesync-replikativ

A filesystem synchronization tool similar to Dropbox over replikativ. It works
similar to git due to [CDVCS](http://replikativ.io/doc/cdvcs.html). To resolve
conflicts you need to drop to the REPL still. You also cannot transfer files
larger than 10 MiB at the moment. 

## Usage

You can set the `:sync-path` to the folder you would like to sync and the
`:store-path` to a folder where replikativ will store the history of changes to
the folder. The `:remotes` are servers which you want to share the data with.
These can also be brokers to connect your machines. Finally pick a different
cdvcs-id (best is random) for each folder, or you will get horrible unintended
conflicts!

~~~clojure
{:store-path "/tmp/sync-test"
 :sync-path "/var/tmp/input"
 :remotes ["ws://localhost:31744"]
 :user "mail:whilo@topiq.es"
 :cdvcs-id #uuid "34db9ec4-82bf-4c61-8e2a-a86294f0e6d4"}
~~~

~~~shell
lein run resources/example-config.edn
~~~

## TODO

- allow OR-Map for different semantics
- fix protocol to handle files in fixed size binary blocks

## License

Copyright © 2016-2017 Christian Weilbach

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

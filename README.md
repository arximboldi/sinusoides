sinusoid.es
===========

[![screenshot](https://cdn.rawgit.com/arximboldi/sinusoides/master/resources/static/screens/sinusoides-thin.svg)](http://sinusoid.es)

This is the code for [my personal webpage](http://sinusoid.es) — my
portfolio and playground for experimentation with web technologies. It
has survived several rewrites since it was first published in 2011.
It is currently built using [ClojureScript](http://clojurescript.net/)
and [Reagent](http://clojurescript.net/).

Development
-----------

### Compile

```
make
```

### Watchers

**TL;DR** run this and go to:
[localhost:8746/debug/](http://localhost:8746/debug/)

```
make dev
```

#### Long version

```
make figwheel
```

Now you have a server on http://localhost:3449 that autoupdates
whenever you touch a file.  When touching the Sass files, one should
do as well:

```
make watch-sass
```

Alternatively, it is possible to `npm install` and then run a local
server with:

```
make serve
```

This creates a server in [localhost:8746](http://localhost:8746) that
also serves the release version of the application.  The debug version
is available on [localhost:8746/debug/](http://localhost:8746/debug/)

### Deployment

```
make upload
```

Fill the password in `host.ncftpput` and remember to not commit it!

License
-------

![license](http://www.gnu.org/graphics/agplv3-155x51.png)

> Copyright (c) 2011-2016 Juan Pedro Bolivar Puente <raskolnikov@gnu.org>
>
> This file is part of Sinusoid.es.
>
> Sinusoid.es is free software: you can redistribute it and/or modify
> it under the terms of the GNU Affero General Public License as
> published by the Free Software Foundation, either version 3 of the
> License, or (at your option) any later version.
>
> Sinusoid.es is distributed in the hope that it will be useful, but
> WITHOUT ANY WARRANTY; without even the implied warranty of
> MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
> Affero General Public License for more details.
>
> You should have received a copy of the GNU Affero General Public
> License along with Sinusoid.es.  If not, see
> <http://www.gnu.org/licenses/>.

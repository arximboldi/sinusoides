.PHONY: all data sass serve

all: data sass cljs

#
# Development
# ===========
#

%.json: %.yaml
	python -c 'import sys, yaml, json; \
		   json.dump(yaml.load(sys.stdin), sys.stdout, indent=4)' \
		< $< > $@

data: data/do.json data/am.json
sass:
	compass compile
cljs:
	lein cljsbuild once

serve:
	coffee server.coffee
watch-sass:
	compass watch
watch-cljs:
	lein cljsbuild auto debug

#
# Deployment
# ==========
#

upload: upload-root upload-build upload-css upload-data upload-views upload-pic

upload-root:
	ncftpput -R -f host.ncftpput / .htaccess
upload-build:
	ncftpput -m -R -f host.ncftpput /build build/*
upload-css:
	ncftpput -m -R -f host.ncftpput /css css/*
upload-data:
	ncftpput -m -R -f host.ncftpput /data data/*
upload-views:
	ncftpput -m -R -f host.ncftpput /views views/*
upload-pic:
	ncftpput -m -R -f host.ncftpput /static/pic static/pic/*

upload-static:
	ncftpput -m -R -f host.ncftpput /static static/*

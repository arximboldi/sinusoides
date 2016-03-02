.PHONY: all data sass serve

all: data sass cljs

#
# Development
# ===========
#

DATA =  resources/data/do.json \
	resources/data/am.json

resources/%.json: %.yaml
	mkdir -p $(@D)
	python -c 'import sys, yaml, json; \
		   json.dump(yaml.load(sys.stdin), sys.stdout, indent=4)' \
		< $< > $@

data: $(DATA)

sass:
	compass compile
cljs:
	lein cljsbuild once
figwheel:
	rlwrap lein figwheel
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

upload: upload-root upload-js upload-css upload-data upload-views upload-pic

upload-root:
	ncftpput -R -f host.ncftpput / resources/.htaccess
upload-js:
	ncftpput -m -R -f host.ncftpput /js resources/js/*
upload-css:
	ncftpput -m -R -f host.ncftpput /css resources/css/*
upload-data:
	ncftpput -m -R -f host.ncftpput /data resources/data/*
upload-views:
	ncftpput -m -R -f host.ncftpput /views resources/views/*
upload-pic:
	ncftpput -m -R -f host.ncftpput /static/pic resources/static/pic/*
upload-static:
	ncftpput -m -R -f host.ncftpput /static resources/static/*

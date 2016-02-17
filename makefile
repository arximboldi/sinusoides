
DATA = data/do.json \
	data/am.json

%.json: %.yaml
	python -c 'import sys, yaml, json; \
		   json.dump(yaml.load(sys.stdin), sys.stdout, indent=4)' \
		< $< > $@

all: $(DATA)

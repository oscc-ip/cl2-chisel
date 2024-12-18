BUILD_DIR = ./build

PRJ = cl2

test:
	./mill -i $(PRJ).test

verilog:
	mkdir -p $(BUILD_DIR)
	./mill -i $(PRJ).runMain Elaborate --target-dir $(BUILD_DIR)

help:
	./mill -i $(PRJ).runMain Elaborate --help

reformat:
	./mill -i __.reformat

checkformat:
	./mill -i __.checkFormat

clean:
	-rm -rf $(BUILD_DIR)

.PHONY: test verilog help reformat checkformat clean

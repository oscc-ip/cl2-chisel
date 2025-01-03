BUILD_DIR = ./build
VSRC_DIR  = ./vsrc

PRJ = cl2

test:
	./mill -i $(PRJ).test

verilog:
	mkdir -p $(VSRC_DIR)
	./mill -i $(PRJ).runMain Elaborate --target-dir $(VSRC_DIR)

help:
	./mill -i $(PRJ).runMain Elaborate --help

reformat:
	./mill -i __.reformat

checkformat:
	./mill -i __.checkFormat

clean:
	-rm -rf $(BUILD_DIR)

fst:
	gtkwave cl2.fst

# verilator simulation
include ./scripts/verilator_sim.mk


.PHONY: test verilog help reformat checkformat clean

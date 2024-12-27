TOP ?= Cl2Core
TOP_FILE_NAME := $(TOP).sv
IMAGE ?=
CSRCS_PATH := $(abspath  ./cl2/src/cc/verilator)

VERILATOR_BUILD_DIR := ./build
OBJ_DIR := $(VERILATOR_BUILD_DIR)/obj_dir
WAVE_DIR := $(VERILATOR_BUILD_DIR)/wave
BIN := $(VERILATOR_BUILD_DIR)/$(TOP)

VSRCS := $(shell find ./vsrc/sv-gen/ -name "*.sv")
CSRCS := $(shell find $(CSRCS_PATH) -name "*.cpp")

VERILATOR_FLAGS +=  -cc --exe \
					--build -j 5 \
					--trace --trace-fst  \
					-Wno-WIDTHEXPAND -Wno-CASEINCOMPLETE

$(BIN):
	@mkdir -p $(VERILATOR_BUILD_DIR)
	@mkdir -p $(WAVE_DIR)
	verilator \
		$(VERILATOR_FLAGS) \
		-top $(TOP)  $(VSRCS) \
		$(CSRCS) \
		--Mdir $(OBJ_DIR) \
		-o $(abspath $(BIN))

sim: $(BIN)
	$(BIN)

.PHONY: sim

TOP ?= Cl2Top
TOP_FILE_NAME := $(TOP).sv
IMAGE ?=
CSRCS_PATH := $(abspath  ./cl2/src/cc/verilator)
CINC_PATH := $(abspath ./cl2/src/cc/verilator/include)

VERILATOR_BUILD_DIR := ./build
OBJ_DIR := $(VERILATOR_BUILD_DIR)/obj_dir
WAVE_DIR := $(VERILATOR_BUILD_DIR)/wave
BIN := $(VERILATOR_BUILD_DIR)/$(TOP)

VSRCS := $(shell find ./vsrc/sv-gen/ -name "*.sv")
CSRCS := $(shell find $(CSRCS_PATH) -name "*.cpp")

CFLAGS := $(addprefix -I, $(CINC_PATH)) -g
LDFLAGS :=
VERILATOR_FLAGS +=  -cc --exe \
					--build -j 5 \
					--trace --trace-fst  \
					--threads 1 \
					-Wno-WIDTHEXPAND -Wno-CASEINCOMPLETE

$(BIN): verilator_clean
	@mkdir -p $(VERILATOR_BUILD_DIR)
	@mkdir -p $(WAVE_DIR)
	verilator \
		$(VERILATOR_FLAGS) \
		-top $(TOP)  $(VSRCS) \
		$(addprefix -CFLAGS , $(CFLAGS)) \
		$(CSRCS) \
		--Mdir $(OBJ_DIR) \
		-o $(abspath $(BIN))

verilator_bin: $(BIN)

verilator_sim: $(BIN)
	$(BIN) example/addi-riscv32-cl2.bin utils/riscv32-spike-so

verilator_gdb: $(BIN)
	gdb --args $(BIN) example/addi-riscv32-cl2.bin utils/riscv32-spike-so

verilator_clean: 
	rm -rf $(VERILATOR_BUILD_DIR)

.PHONY: verilator_sim verilator_bin verilator_gdb verilator_clean

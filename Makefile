# Project Configuration
PRJ         = cl2
PRJ_DIR     = $(CURDIR)
BUILD_DIR   = ./build
VSRC_DIR    = ./vsrc
WAVE        = cl2.fst
TEST_BUILD  = ./test/riscv-arch-test-am/build

# Makefile Paths
VERILATOR_MAKE  = ./scripts/verilator_sim.mk
RVTEST_MAKE     = ./test/riscv-arch-test-am/Makefile

# Tools
MILL       = mill
MKDIR      = mkdir -p
RM         = rm -rf
DUMPWAVE   = gtkwave
MAKE       = make

# Export Variables
export PRJ_DIR

# Phony Targets
.PHONY: all verilog gen help reformat checkformat clean fst run gdb latest

# Default Target
all: gen

# Generate Verilog
verilog:
	@echo "Generating Verilog files..."
	$(MKDIR) $(VSRC_DIR)
	$(MILL) -i $(PRJ).runMain Elaborate --target-dir $(VSRC_DIR)

# Generate Binary (depends on verilog)
gen: verilog
	@echo "Generating binary using Verilator..."
	$(MAKE) -f $(VERILATOR_MAKE) verilator_bin

# Show Help for Elaborate
help:
	@echo "Displaying help for Elaborate..."
	$(MILL) -i $(PRJ).runMain Elaborate --help

# Reformat Code
reformat:
	@echo "Reformatting code..."
	$(MILL) -i __.reformat

# Check Code Format
checkformat:
	@echo "Checking code format..."
	$(MILL) -i __.checkFormat

# Clean Build Artifacts
clean:
	@echo "Cleaning build artifacts..."
	$(RM) $(BUILD_DIR) $(TEST_BUILD) $(WAVE)

# Open Waveform
fst:
	@echo "Opening waveform with gtkwave..."
	$(DUMPWAVE) $(WAVE)

# Test Targets (run, gdb, latest)
run gdb latest:
	@echo "Running target '$@' for ARCH=$(ARCH)..."
	$(MAKE) -C test/riscv-arch-test-am ARCH=$(ARCH) $@
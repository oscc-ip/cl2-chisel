
WORK_DIR  = $(shell pwd)
DST_DIR   = $(WORK_DIR)/build/$(ARCH)
$(shell mkdir -p $(DST_DIR))

CL2_HOME := $(abspath $(WORK_DIR)../../../)

IMAGE_REL = build/$(NAME)-cl2
IMAGE     = $(abspath $(IMAGE_REL))
ARCH      = riscv32

SRCS += ../common/start.S ../common/trm.c
OBJS      = $(addprefix $(DST_DIR)/, $(addsuffix .o, $(basename $(SRCS))))

COMMON_CFLAGS := -fno-pic -march=rv32im -mcmodel=medany  -mstrict-align -mabi=ilp32
CROSS_COMPILE := riscv32-unknown-elf-


AS        = $(CROSS_COMPILE)gcc
CC        = $(CROSS_COMPILE)gcc
CXX       = $(CROSS_COMPILE)g++
LD        = $(CROSS_COMPILE)ld
AR        = $(CROSS_COMPILE)ar
OBJDUMP   = $(CROSS_COMPILE)objdump
OBJCOPY   = $(CROSS_COMPILE)objcopy
READELF   = $(CROSS_COMPILE)readelf

INC_PATH += $(WORK_DIR)/include
INCFLAGS += $(addprefix -I, $(INC_PATH))

# C/C++ FLAGS
CFLAGS   += -lm -g -O3 -MMD -Wall $(INCFLAGS) \
            -fno-asynchronous-unwind-tables -fno-builtin -fno-stack-protector \
            -Wno-main -U_FORTIFY_SOURCE -fvisibility=hidden
CFLAGS    += -fdata-sections -ffunction-sections
CXXFLAGS +=  $(CFLAGS) -ffreestanding -fno-rtti -fno-exceptions

# AS FLAGS
ASFLAGS  += -MMD $(INCFLAGS)

# LD FLAGS
LDFLAGS  += -z noexecstack
LDFLAGS += -melf32lriscv
LDFLAGS   += -T $(CL2_HOME)/scripts/linker.ld \
						 --defsym=_pmem_start=0x80000000 --defsym=_entry_offset=0x0
LDFLAGS   += --gc-sections -e _start

$(DST_DIR)/%.o: %.c
	@mkdir -p $(dir $@) && echo + CC $<
	@$(CC) -std=gnu11 $(CFLAGS) -c -o $@ $(realpath $<)

$(DST_DIR)/%.o: %.cc
	@mkdir -p $(dir $@) && echo + CXX $<
	@$(CXX) -std=c++17 $(CXXFLAGS) -c -o $@ $(realpath $<)

$(DST_DIR)/%.o: %.cpp
	@mkdir -p $(dir $@) && echo + CXX $<
	@$(CXX) -std=c++17 $(CXXFLAGS) -c -o $@ $(realpath $<)

$(DST_DIR)/%.o: %.S
	@mkdir -p $(dir $@) && echo + AS $<
	@$(AS) $(ASFLAGS) -c -o $@ $(realpath $<)

$(IMAGE).elf: $(OBJS)
	@echo + LD "->" $(IMAGE_REL).elf
	@$(LD) $(LDFLAGS) -o $(IMAGE).elf --start-group $(OBJS) --end-group

-include $(addprefix $(DST_DIR)/, $(addsuffix .d, $(basename $(SRCS))))

image: image-dep $(IMAGE).elf
	@$(OBJDUMP) -d $(IMAGE).elf > $(IMAGE).txt
	@echo + OBJCOPY "->" $(IMAGE_REL).bin
	@$(OBJCOPY) -S --set-section-flags .bss=alloc,contents -O binary $(IMAGE).elf $(IMAGE).bin
image-dep: $(OBJS)
	@echo \# Creating image
.PHONY: image image-dep run

clean:
	rm -rf Makefile.html $(WORK_DIR)/build/
.PHONY: clean

ref= $(CL2_HOME)/utils/riscv32-spike-so
run: image
	$(CL2_HOME)/build/Cl2Top $(IMAGE).bin $(ref)
gdb: image
	gdb --args $(CL2_HOME)/build/Cl2Top $(IMAGE).bin $(ref)

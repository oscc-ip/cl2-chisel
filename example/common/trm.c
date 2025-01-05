#include <stdint.h>

#define RANGE(st, ed)       (Area) { .start = (void *)(st), .end = (void *)(ed) }
#define PMEM_SIZE (128 * 1024 * 1024)
#define PMEM_END  ((uintptr_t)&_pmem_start + PMEM_SIZE)

extern char _heap_start;
extern char _pmem_start;
extern int main(const char *args);

// Memory area for [@start, @end)
typedef struct {
  void *start, *end;
} Area;


Area heap = RANGE(&_heap_start, PMEM_END);

#ifndef MAINARGS
#define MAINARGS ""
#endif
static const char mainargs[] = MAINARGS;

void halt(int code) {
    asm volatile("mv a0, %0; ebreak" :: "r"(code));

    //can't compile without this
    while (1);
}

void _trm_init() {
  int ret = main(mainargs);
  halt(ret);
}

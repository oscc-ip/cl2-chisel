#include <stdint.h>

int main() __attribute__((optimize("O0")));
int main() {
  int dummy = 1 + 3;
  dummy--;
  dummy++;


  dummy = 0;
  return dummy / 2;
}

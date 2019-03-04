#include "zynq_driver.h"
#include "fesvr/tsi.h"
#include <vector>

#define BLKDEV_NTAGS 2

int main(int argc, char** argv)
{
    tsi_t tsi(argc, argv);

    BlockDevice *blkdev = NULL;
    zynq_driver_t *driver;

    for (int i = 1; i < argc; i++) {
        const char *name = NULL;

        if (strncmp(argv[i], "+blkdev=", 8) == 0) {
            name = argv[i] + 8;
            blkdev = new BlockDevice(name, BLKDEV_NTAGS);
        }
    }

    driver = new zynq_driver_t(&tsi, blkdev);

    while(!tsi.done()){
        driver->poll();
    }

    delete driver;
    if (blkdev != NULL)
        delete blkdev;

    return tsi.exit_code();
}

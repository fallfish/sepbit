# Guidance for preparing the emulated zoned device

* We introduce how we prepare the emulated zoned device in this document

## Optane PMM
* Create a block device and format it using ext4
    * ``ndctl create-namespace --mode=fsdax``
    * ``mkfs.ext4 /dev/pmem0``
* Mount the PMM-based block device
    * ``mount -o dax /dev/pmem0 /mnt/pmem0``

## Emulated zoned device
* Tool: [tcmu-runner](https://github.com/open-iscsi/tcmu-runner)
* Tool: [targetcli](https://github.com/open-iscsi/targetcli-fb)
* Make sure to run a tcmu-runner daemon before using targetcli to configure user:zbc backstores
* Create a Host-Managed SMR user:zbc target
    * Using targetcli under /backstores/user:zbc
    * ``create name=zbc0 size=400g cfgstring=model-HM/zsize-256/conv-10@/mnt/pmem0/zbc1.raw``
* Create a loopback device based on a file (on PMM) using targetcli
    * Using targetcli under /loopback; create a device, under its luns/ folder
    * ``create /backstores/user:zbc/zbc0 0``
    * Use ``lsscsi -g`` to check and a new HM-SMR device (e.g., /dev/sdd) is prepared

# java-paperang-p2-usb
Invoke paperang p2 using USB in Java

## How to use?

First, do not use Windows. Windows is suck. I'm using a openSUSE VM and forward
the paperang P2 usb into the vm, then use IDEA to run the program in the VM. By
doing so, usb4java and javax.usb can directly open the USB without installing
any special drivers that will freeze all usb ports on Windows 10. 

If you're a linux user, congratulations. Just run the [Main.kt](./src/main/kotlin/info/skyblond/paperang/Main.kt),
and it will print you a test pattern. The pattern is used to test line length,
dots per line, dots per column.

To test line length, the program will print columns every 8 bytes, then count the columns
and multiplied by 8, I got 72 bytes per line for P2. P1 is 48 bytes per line.

To test dots per line/column, the program will print an alternating pattern, like
`10101010`, if you count 4 columns or 7 lines, then it means each bit corresponding
to each dot.

Finally, it will print some random data to test multi packet.

## What's next?

Paperang's user license said user cannot reverse engineer on the software, which
means to me, is, I can reverse engineer on hardware. However, I write a lot of Java
and hardly work with hardware. So, unless you reverse engineer their apk file,
it's extremely hard to figure out the whole list of commands and their parameters.

The packet format is:

```
 ┌────┬───────────┬────────────────┬─────────────────┬─────────────┬──────────┬────┐
 │0x02│command(1B)│packetRemain(1B)│dataLength(2B,LE)│data(0~1023B)│CRC(4B,LE)│0x03│
 └────┴───────────┴────────────────┴─────────────────┴─────────────┴──────────┴────┘
```

All packets start with `0x02`, then followed by 1 byte of command. Then 1 byte of
remain packets counter, `0x03` means there are still 3 packets to be sent, and
`0x00` means this packet is the last packet. Data length is 2 byte long, aka, a
`Short` represented in little endian, for example, `0x1234` will be `3412`. Then
followed by data, if data length is `0x00`, then data can be missing, otherwise
the max length is 1023 bytes. CRC is not standard, this field is CRC32 of the data.
The standard CRC32 has an initial value of `0x00000000`, but paperang use `0x35769521`.
The CRC32 is an integer and still need to store in little endian. The packet always
end with `0x03`.

To print something, all you need to do is:

1. Set the paper type to normal paper, by sending command `0x2c`, data: `0x00`
2. Feed some space before print, by sending command `0x1a`, data: some integer in LE, like 50
3. Send the print data using multi packet, by sending command `0x00`, and the data.
4. Finally, feed some paper so you can remove the result, by sending command `0x1a`, data: some integer in LE, like 300

The data is serial in packet, aka, the first packet will be print, then second,
..., until last packet is sent.
The print data is essentially a black and white bitmap. You can print whatever you want.
And with Java, you can make a springboot service to receive data and print it out.
**Much better than python** :)

However, this is not the end of the story. Paperang at least has 40+ commands, where
you can set how deep (print temp) the print is, get battery level, see machine status, etc.

## Reference

I don't have any hardware debug tools on my hand, so all info in this project is from
the internet and the following projects:

+ [ampatspell/paperang](https://github.com/ampatspell/paperang): Invoke paperang
in NodeJS using webUSB, not working, but useful.
+ [tinyprinter/python-paperang](https://github.com/tinyprinter/python-paperang): Invoke
paperang in Python through bluetooth, I don't like python, but the info is every useful.
+ [喵喵机折腾记](https://www.ihcblog.com/miaomiaoji/): A Chinese blog post gives the info
about the packet format.
+ [Taking a ride on the Universal Serial Bus](https://maff.scot/2019/10/taking-a-ride-on-the-universal-serial-bus/):
An English blog post about invoking P1 using USB.

Thanks to all of those references! I can't build this project without them.

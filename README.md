# java-paperang-p2-usb
Invoke paperang p2 using USB in Java

## How to use?

First thing: do not use Windows. Windows is suck. I'm using a openSUSE VM and forward
the paperang P2 usb into the vm, then use IDEA to run the program in the VM. By
doing so, usb4java and javax.usb can directly open the USB without installing
any special drivers that will freeze all usb ports on Windows 10. 

If you're a linux user, congratulations.

### Print test pattern

The first thing you want to do is print some test pattern and make sure this
program is still functioning. The [UsbPatternTest.kt](./src/main/kotlin/info/skyblond/paperang/run/UsbPatternTest.kt)
use the raw USB communication and try it best to not use advanced method wrote in
this project. The idea is make sure the USB communication is working, before using
other codes based on that.

Run the [UsbPatternTest.kt](./src/main/kotlin/info/skyblond/paperang/run/UsbPatternTest.kt),
and it will print you a test page and some patterns. The patterns are used to test line length,
dots per line, dots per column.

To test line length, the program will print columns every 8 bytes, then count the columns
and multiplied by 8, I got 72 bytes per line for P2. P1 is 48 bytes per line.

To test dots per line/column, the program will print an alternating pattern, like
`10101010`, if you count 4 columns or 7 lines, then it means each bit corresponding
to each dot.

Finally, it will print some random data to test multi packet.

### Test heat density settings

If your result is super light, you might need to change the heat density setting.
The max density is 100 and the minimal is 0. When setting to 0, it's not pure white,
the print result is very light, and might not last long.

[UsbHeatDensityTest.ky](./src/main/kotlin/info/skyblond/paperang/run/UsbHeatDensityTest.kt)
use the command to set the heat density to 75, and then print the test page.

### What about print some text?

Since all this printer needed is a bitmap, so I came up with a simple typesetting
system at [Typesetting.kt](./src/main/kotlin/info/skyblond/paperang/Typesetting.kt).
It defines some basic unit like `Word`, a non-splittable unit when printing; `Tab` &
`Space`, just like the name, it represents a `\t` and space; also `Backspace`, where
you can rewind the cursor to overwrite something. The typesetting system is linear,
which means it treats words like a line and put them line by line. You might need
LaTeX or photoshop for more advanced things.

[TypesettingTest.kt](./src/main/kotlin/info/skyblond/paperang/run/TypesettingTest.kt) will
generate a 1bit color png file (since the bitmap don't have gray). To print it,
you can run [UsbPrintTextPicTest.kt](./src/main/kotlin/info/skyblond/paperang/run/UsbPrintTextPicTest.kt)),
it shows you how to read a pic and packet it into printer-understandable data and
print it. By default, it prints the result of [TypesettingTest.kt](./src/main/kotlin/info/skyblond/paperang/run/TypesettingTest.kt).

## What's next?

Paperang's user license said user cannot reverse engineer on the software, which
means to me, is, I can reverse engineer on hardware. However, I write a lot of Java
and hardly work with hardware. So, (in my opinion) unless you reverse engineer their apk file,
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
2. Set the heat density, by sending command `0x19`, data: some short in LE, like 75
3. Feed some space before print, by sending command `0x1a`, data: some short in LE, like 50
4. Send the print data using multi packet, by sending command `0x00`, and the data.
5. Finally, feed some paper, so you can remove the result, by sending command `0x1a`, data: some short in LE, like 300

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

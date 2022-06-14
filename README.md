## NhNotifier

Windows notifications for nh.

### Installation

Download the jar file from https://github.com/log1x0/NhNotifier/releases

You need Java >= 17. I recommend Java 17. (The Java path should be entered
correctly in the system path.)

Make sure, Java is working by entering `java -version`.

### Usage

Open CMD and then:

`cd Downloads`

`java -jar NhNotifier.jar <u> <p>`

replace `<u>` and `<p>` with `uid` and `pass` found in your browser.

Stop everything with <kbd>Ctrl</kbd>+<kbd>C</kbd>.

### Create a desktop shortcut:

Open the windows exloprer and go to the java bin folder ("C:\Program Files\Java\jdk-xxx\bin"). Right click on "java.exe" and select "Send to" and "Create dektop shortcut". Then right click on the new desktop symbol and select "Properties". Add the following to "Target": ` -jar "C:\Users\xxx\Downloads\NhNotifier.jar" 123 abc`. Replace xxx, 123 and abc with the correct path. That's all.

import 'dart:typed_data';
import 'dart:ui' as ui;
import 'dart:async';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/painting.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      debugShowCheckedModeBanner: false,
      darkTheme:
          ThemeData.dark().copyWith(scaffoldBackgroundColor: Colors.black87),
      theme: ThemeData.light(),
      themeMode: ThemeMode.dark,
      home: MyHome(),
    );
  }
}

class MyHome extends StatefulWidget {
  @override
  _MyHomeState createState() => _MyHomeState();
}

class _MyHomeState extends State<MyHome> {
  static const platform = MethodChannel('flirCamera');

  String _discovered = 'None Found';

  bool _hasCamera = false;

  bool _connected = false;

  Timer timer;
  int _start = 5;

  ui.Image _image;

  double _temp;

  bool _alert = false;

  List<String> _status = List<String>();

  @override
  void initState() {
    super.initState();

    platform.invokeMethod('cleanAll');

    platform.setMethodCallHandler((MethodCall call) async {
      switch (call.method) {
        case 'temperature':
          setState(() {
            _temp = call.arguments;
          });
          break;
        case 'discovered':
          if (call.arguments) {
            setState(() {
              _discovered = 'Camera Found';
              _hasCamera = true;
            });
          }
          break;
        case 'connected':
          setState(() {
            _connected = call.arguments;
          });
          break;
        case 'streamBytes':
          _getBitmap(call.arguments);
          break;
        case 'streamFinished':
          setState(() {
            _image = null;
            _temp = null;
          });
          break;
        default:
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_image != null && _temp != null) setTemp();
    if (_alert)
      showDialog(
          context: context,
          barrierDismissible: false,
          builder: (_) => AlertDialog(
                title: _determineAlertDialogTitle(_temp),
                content: _determineAlertDialogText(_temp),
                actions: <Widget>[
                  FlatButton(
                      onPressed: () {
                        Navigator.of(_).pop();
                      },
                      child: Text("Ok"))
                ],
              ));

    return SafeArea(
        child: Scaffold(
      body: Stack(
        fit: StackFit.expand,
        children: <Widget>[
          if (_image != null)
            CustomPaint(
              painter: StreamPainter(_image),
            ),
          if (_image != null)
            Align(
              alignment: Alignment.center,
              child: SizedBox(
                height: 100,
                width: 100,
                child: CustomPaint(
                  painter: RectanglePaint(_image),
                ),
              ),
            ),
          if (_image == null)
            Align(
              alignment: Alignment.topCenter,
              child: Text(
                '$_discovered',
                style: TextStyle(fontSize: 35),
              ),
            ),
          if (!_hasCamera)
            Align(
              alignment: Alignment.bottomCenter,
              child: CupertinoButton(
                  onPressed: () {
                    _startDiscover();
                  },
                  child: Text('Start discover')),
            ),
          if (_hasCamera && !_connected)
            Align(
              alignment: Alignment.bottomCenter,
              child: CupertinoButton(
                  child: Text('Connect to FLIR ONE'),
                  onPressed: () {
                    _connect();
                  }),
            ),
          if (_connected && _image == null)
            Align(
              alignment: Alignment.bottomCenter,
              child: Padding(
                padding: const EdgeInsets.only(bottom: 50),
                child: CupertinoButton.filled(
                    child: Text('Iniciar'),
                    onPressed: () {
                      platform.invokeMethod('startStream');
                    }),
              ),
            ),
          if (_connected && _image == null)
            Align(
              alignment: Alignment.bottomCenter,
              child: CupertinoButton(
                  child: Text('Desconectar'),
                  onPressed: () {
                    _disconnect();
                  }),
            ),
          if (_image != null)
            Align(
              alignment: Alignment.bottomCenter,
              child: Padding(
                padding: const EdgeInsets.only(bottom: 8.0),
                child: CupertinoButton.filled(
                    child: Text("Parar"),
                    onPressed: () {
                      _stopStream();
                    }),
              ),
            ),
          if (_image != null)
            Align(
              alignment: Alignment.topCenter,
              child: Text(
                "$_start",
                style: TextStyle(fontSize: 20),
              ),
            ),
          if (_image != null && _temp != null)
            Align(
              alignment: Alignment.center,
              child: Text("$_temp"),
            )
        ],
      ),
    ));
  }

  Future<void> _startDiscover() async {
    await platform.invokeListMethod('discover');
  }

  Future<void> _connect() async {
    await platform.invokeMethod('connect');
  }

  void _disconnect() async {
    bool disconnected = await platform.invokeMethod('disconnect');
    setState(() {
      _connected = !disconnected;
    });
  }

  @override
  void dispose() {
    super.dispose();
    if (_image != null) _stopStream();
    platform.invokeMethod("cleanAll");
  }

  Future<void> _getBitmap(Uint8List data) async {
    ui.Image img = await _getUiImage(data);
    setState(() {
      _image = img;
    });
  }

  void _stopStream() async {
    platform.invokeMethod("stopStream");
  }

  Map<String, Color> statusColor = Map<String, Color>.from(
      {'normal': Colors.green, 'high': Colors.yellow, 'fever': Colors.red});

  String _getTemperatureStatus(double temp) {
    if (temp > 38) return 'fever';
    if (temp > 37) return 'high';
    return 'normal';
  }

  void setTemp() async {
    await Future.delayed(Duration(seconds: 1));
    if (_start < 1) {
      setState(() {
        setState(() {
          _alert = true;
        });
      });
    }
    setState(() {
      _start = _start - 1;
    });
  }

  Widget _determineAlertDialogText(double temp) {
    String msg;
    if (temp > 38) {
      msg = "Estado febril, procure um médico";
    }
    if (temp > 37) {
      msg = "Temperatura acima do normal";
    }
    msg = "Temperatura normal";

    return Text(msg);
  }

  Widget _determineAlertDialogTitle(double temp) {
    Icon icon;
    if (temp > 38) {
      icon = Icon(
        Icons.error,
        color: Colors.red,
        size: 40,
      );
    }
    if (temp > 37) {
      icon = Icon(
        Icons.warning,
        color: Colors.yellow,
        size: 40,
      );
    }
    icon = Icon(
      Icons.check_circle,
      color: Colors.green,
      size: 40,
    );

    return icon;
  }
}

Future<ui.Image> _getUiImage(Uint8List data) async {
  return await decodeImageFromList(data);
}

class StreamPainter extends CustomPainter {
  final ui.Image data;
  StreamPainter(this.data);

  @override
  void paint(Canvas canvas, Size size) {
    // canvas.drawRect(Rect.fromLTWH(0, 0, data.width/2, data.height/2), Paint()..color = Colors.white);
    paintImage(
        canvas: canvas,
        rect: Rect.fromCenter(
            center: size.center(Offset.zero),
            height: size.height,
            width: size.width),
        image: data);
  }

  @override
  bool shouldRepaint(CustomPainter oldDelegate) => true;
}

class RectanglePaint extends CustomPainter {
  final ui.Image img;

  RectanglePaint(this.img);

  @override
  void paint(Canvas canvas, Size size) {
    final Offset zero = size.center(Offset.zero);
    final Paint paint = Paint()
      ..color = Colors.white
      ..style = PaintingStyle.stroke
      ..strokeWidth = 3;
    canvas.drawRect(
        Rect.fromCenter(center: zero, height: 100, width: 100), paint);
  }

  @override
  bool shouldRepaint(CustomPainter oldDelegate) => true;
}

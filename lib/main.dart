import 'dart:typed_data';
import 'dart:ui' as ui;
import 'dart:async';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/painting.dart';

void main() => SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]).then((_) => runApp(MyApp()));

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Câmera Térmica',
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

  String _discovered = 'Câmera indisponível';

  bool _hasCamera = false;

  bool _connected = false;

  ui.Image _image;

  double _temp;

  bool _show = false;
  int _seconds = 0;
  int _secondsLimit = 2;

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
          setTemp(call.arguments);
          break;
        case 'discovered':
          if (call.arguments) {
            setState(() {
              _discovered = 'Câmera disponível';
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
    return SafeArea(
        child: Scaffold(
      body: Stack(
        fit: StackFit.expand,
        children: <Widget>[
          ...discovery(),
          ...connect(),
          if (_image != null) ...stream()
        ],
      ),
    ));
  }

  List<Widget> discovery() {
    return [
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
              child: Text('Procurar câmera')),
        ),
      if (_hasCamera && !_connected)
        Align(
          alignment: Alignment.bottomCenter,
          child: CupertinoButton(
              child: Text('Connectar à câmera'),
              onPressed: () {
                _connect();
              }),
        ),
    ];
  }

  List<Widget> stream() {
    return [
      CustomPaint(
        painter: StreamPainter(_image),
      ),
      CustomPaint(
        painter: RectanglePaint(),
      ),
      Align(
        alignment: Alignment.topCenter,
        child: Text("$_seconds", style: TextStyle(fontSize: 30),),
      ),
    ];
  }

  List<Widget> connect() => [
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
      ];

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

  Future<void> setTempStats(double temperature) async {
    return await Future.delayed(Duration(seconds: 1), () {
      if (temperature < 30) {
        setState(() {
          _show = false;
          _seconds = 0;
        });
        return;
      }
      if (_seconds > _secondsLimit - 1) {
        showDialog(
            context: context,
            barrierDismissible: false,
            useRootNavigator: true,
            builder: (_) => AlertDialog(
                  title: _determineAlertDialogTitle(temperature),
                  content: _determineAlertDialogText(temperature),
                )).then((_) {
          setState(() {
            _seconds = 0;
            _show = false;
          });
        });
        Future.delayed(Duration(seconds: 2), () => Navigator.pop(context));
        return;
      }
      setState(() {
        _seconds = _seconds + 1;
        _show = false;
      });
    });
  }

  Future<void> setTemp(double temp) async {
    if (_image != null && _temp != null && !_show) {
      setState(() {
        _show = true;
      });
      await setTempStats(temp);
    }
  }

  Widget _determineAlertDialogText(double temp) {
    if (temp >= 37.7) return Text("Estado febril, procure um médico");

    if (temp >= 37 && temp <= 37.7) return Text("Temperatura acima do normal");

    return Text("Temperatura normal");
  }

  Widget _determineAlertDialogTitle(double temp) {
    if (temp >= 38)
      return Icon(
        Icons.error,
        color: Colors.red,
        size: 40,
      );
    if (temp >= 37 && temp <= 37.7)
      return Icon(
        Icons.warning,
        color: Colors.yellow,
        size: 40,
      );
    return Icon(
      Icons.check_circle,
      color: Colors.green,
      size: 40,
    );
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
    paintImage(
      canvas: canvas,
      image: data,
      rect: Rect.fromCenter(
          center: size.center(Offset.zero),
          height: size.height,
          width: size.width),
    );
  }

  @override
  bool shouldRepaint(CustomPainter oldDelegate) => true;
}

class RectanglePaint extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final Offset zero = size.center(Offset(0, -25));
    final Paint paint = Paint()
      ..color = Colors.white
      ..style = PaintingStyle.stroke
      ..strokeWidth = 3;
    canvas.drawRect(
        Rect.fromCenter(center: zero, height: 50, width: 50), paint);
  }

  @override
  bool shouldRepaint(CustomPainter oldDelegate) => true;
}

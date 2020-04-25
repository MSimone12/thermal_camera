import 'dart:typed_data';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:thermal_camera/CameraModal.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      debugShowCheckedModeBanner: false,
      darkTheme: ThemeData.dark(),
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

  Uint8List _bitmap;

  bool _connected = false;

  List<Widget> _cams = List<Widget>();

  @override
  void initState() {
    super.initState();
    _startDiscover();

    platform.setMethodCallHandler((MethodCall call) async {
      switch (call.method) {
        case 'stream':
          setState(() {
            _bitmap = call.arguments;
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
          body: Container(
        constraints: BoxConstraints.expand(),
        child: Column(
          children: <Widget>[
            Padding(
              padding: const EdgeInsets.all(8.0),
              child: Text(
                'Flir One Camera',
                style: TextStyle(fontSize: 40),
              ),
            ),
            Text(
              '$_discovered',
              style: TextStyle(fontSize: 35),
            ),
            Column(
              children: _cams.length > 0
                  ? [
                      Text(
                        'Connect',
                        style: TextStyle(fontSize: 30),
                      ),
                      CupertinoButton(
                          child: Text('Connect to FLIR ONE'),
                          onPressed: () {
                            _connect();
                          }),
                    ]
                  : [Container()],
            ),
            _connected
                ? Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: <Widget>[
                      Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: CupertinoButton.filled(
                            child: Text('iniciar'),
                            onPressed: () {
                              platform.invokeMethod('startStream');
                              Navigator.of(context).push(
                                  PageRouteBuilder(pageBuilder: (ctx, _, __) {
                                return FadeTransition(
                                  opacity: _,
                                  child: CameraModal(data: _bitmap),
                                );
                              })).whenComplete(() async {
                                _disconnect();
                              });
                            }),
                      ),
                      Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: CupertinoButton.filled(
                            child: Text('Desconectar'),
                            onPressed: () {
                              _disconnect();
                            }),
                      ),
                    ],
                  )
                : Container(),
          ],
        ),
      )),
    );
  }

  Future<void> _startDiscover() async {
    List<dynamic> discovered = await platform.invokeListMethod('discover');
    if (discovered.length > 0) {
      setState(() {
        _discovered = 'FLIR ONE found!';
      });
    }
  }

  Future<void> _connect() async {
    bool connected = await platform.invokeMethod('connect');
    setState(() {
      _connected = connected;
    });
  }

  Future<void> _disconnect() async {
    bool disconnected = await platform.invokeMethod('disconnect');
    setState(() {
      _connected = !disconnected;
    });
  }
}

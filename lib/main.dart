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
      theme: ThemeData.light(),
      themeMode: ThemeMode.light,
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

  Uint8List _bitmap;

  bool _connected = false;

  List<Widget> _cams = List<Widget>();

  @override
  void initState() {
    super.initState();

    platform.setMethodCallHandler((MethodCall call) async {
      switch (call.method) {
        case 'stream':
          setState(() {
            _bitmap = call.arguments;
          });
          break;
        case 'discovered':
          if (call.arguments.length > 0) {
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
            !_hasCamera ? FlatButton(
                onPressed: () {
                  _startDiscover();
                },
                child: Text('Start discover')) : Container(),
            Column(
              children: _hasCamera && !_connected
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
                              // Navigator.of(context).push(
                              //     PageRouteBuilder(pageBuilder: (ctx, _, __) {
                              //   return FadeTransition(
                              //     opacity: _,
                              //     child: SafeArea(
                              //       child: Scaffold(
                              //         body: Stack(
                              //           fit: StackFit.expand,
                              //           children: <Widget>[
                              //             Align(
                              //               alignment: Alignment.topLeft,
                              //               child: IconButton(
                              //                   icon: Icon(Icons.close),
                              //                   onPressed: () {
                              //                     Navigator.of(context).pop();
                              //                     _disconnect();
                              //                   }),
                              //             ),
                              //             Container(
                              //               constraints:
                              //                   BoxConstraints.expand(),
                              //               child: _bitmap != null
                              //                   ? Image.memory(_bitmap, fit: BoxFit.contain,)
                              //                   : Container(),
                              //             )
                              //           ],
                              //         ),
                              //       ),
                              //     ),
                              //   );
                              // })).whenComplete(() async {
                              //   _disconnect();
                              // });
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

                _bitmap != null ? Image.memory(_bitmap, width: 100, height: 200, fit: BoxFit.contain,): Container(),
          ],
        ),
      )),
    );
  }

  Future<void> _startDiscover() async {
    await platform.invokeListMethod('discover');
  }

  Future<void> _connect() async {
    await platform.invokeMethod('connect');
  }

  Future<void> _disconnect() async {
    bool disconnected = await platform.invokeMethod('disconnect');
    setState(() {
      _connected = !disconnected;
    });
  }

  @override
  void dispose() {
    super.dispose();
    _disconnect();
  }
}

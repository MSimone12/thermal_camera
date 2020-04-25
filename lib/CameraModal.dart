import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class CameraModal extends StatefulWidget {
  final Uint8List data;

  CameraModal({this.data});

  @override
  _CameraModalState createState() => _CameraModalState();
}

class _CameraModalState extends State<CameraModal> {
  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Scaffold(
        body: Stack(
          fit: StackFit.expand,
          children: <Widget>[
            Align(
              alignment: Alignment.topLeft,
              child: IconButton(
                  icon: Icon(Icons.close),
                  onPressed: () {
                    Navigator.of(context).pop();
                  }),
            ),
            widget.data != null
                ? Image.memory(
                    widget.data,
                    fit: BoxFit.fill,
                  )
                : Container(),
          ],
        ),
      ),
    );
  }
}

Option Explicit

Dim shell, fileSystem, root, command, answer

answer = MsgBox( _
    "Esto cerrara el programa en esta computadora." & vbCrLf & vbCrLf & _
    "La caja operativa no se cerrara automaticamente. Termina las ventas pendientes y cierra la caja desde el sistema antes de continuar.", _
    vbQuestion + vbYesNo + vbDefaultButton2, _
    "Cerrar La Ferreteria" _
)

If answer <> vbYes Then
    WScript.Quit 0
End If

Set shell = CreateObject("WScript.Shell")
Set fileSystem = CreateObject("Scripting.FileSystemObject")

root = fileSystem.GetParentFolderName(WScript.ScriptFullName)
command = "powershell.exe -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File """ & root & "\cerrar.ps1"""

shell.Run command, 0, False

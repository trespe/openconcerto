if Wscript.Arguments.count = 0 then
  Wscript.Echo "Usage: " & WScript.ScriptName & " shortcutPath [absoluteTarget]"
  Wscript.Quit
end if

linkPath = Wscript.Arguments.item(0)
' a shortcut must always end with .LNK
if UCase(Right(linkPath, 4)) <> ".LNK" then
  linkPath = linkPath & ".LNK"
end if

Set oWS = WScript.CreateObject("WScript.Shell")
Set oLink = oWS.CreateShortcut(linkPath)

if Wscript.Arguments.count = 1 then
' means readlink
  Wscript.Echo oLink.TargetPath
else
' means ln
  targetPath = Wscript.Arguments.item(1)
  oLink.TargetPath = targetPath
  'oWS.CurrentDirectory & "\" & 
   '	oLink.Arguments = ""
   '	oLink.Description = "MyProgram"
   '	oLink.HotKey = "ALT+CTRL+F"
   '	oLink.IconLocation = "C:\Program Files\MyApp\MyProgram.EXE, 2"
   '	oLink.WindowStyle = "1"
   '	oLink.WorkingDirectory = "C:\Program Files\MyApp"
  oLink.Save
end if

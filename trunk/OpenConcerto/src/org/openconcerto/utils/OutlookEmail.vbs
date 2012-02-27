' http://msdn.microsoft.com/en-us/library/aa210946(office.11).aspx

' Enable error handling
On Error Resume Next

if Wscript.Arguments.count = 0 or Wscript.Arguments.Named.count > 3 then
	Wscript.Echo "Usage: " & WScript.ScriptName & " /to:addr /subject:Re [ /body:""Hi, dear friend"" | /unicodeStdIn:[0|1] ] attachment..." & vbNewLine &_
	vbTab & "Named parameters should be percent-encoded since certain characters like double quote cannot be passed." &_
	" If unicodeStdIn is defined the body will be read from stdin (avoiding arguments size limitation), 1 will parse the stream as UTF16, " &_
	"0 will parse as the platform default. If calling from cmd.exe you might need to change the codepage, e.g. chcp 1252 (from 850)"
	Wscript.Quit 1
end if

toAddr = getNamedArg("to")
subject = getNamedArg("subject")
' Cannot always call StdIn.ReadAll since it blocks 
If Wscript.Arguments.Named.Exists("unicodeStdIn") then
	isUnicode = CBool(Wscript.Arguments.Named.item("unicodeStdIn"))
	Set fso = CreateObject ("Scripting.FileSystemObject")
	Set ins =  fso.GetStandardStream (StdIn, isUnicode)
	' ReadAll fails if already at the end of file
	if ins.AtEndOfStream then
		piped = ""
	else
		piped = ins.ReadAll
	end if
else
	body = getNamedArg("body")
End If

quitIfErr()

'Create a mail object and send the mail
Dim objMail
Dim objclient

Set objMail = CreateObject("Outlook.application")
quitIfErr()
Set objclient = objMail.createitem(olMailItem)
quitIfErr()

With objclient
	.Subject = subject 
	.To = toAddr
	'.CC = "cc@email.com"
	.Body = body & piped
	For each attachmentPath in Wscript.Arguments.Unnamed
		.Attachments.Add attachmentPath
	Next
	.Display
End With
quitIfErr()

Function getNamedArg(n)
	getNamedArg = Unescape(Wscript.Arguments.Named.item(n))
End Function

Function getNoExn(i)
	if Wscript.Arguments.Unnamed.count > i then
		getNoExn = Wscript.Arguments.Unnamed.item(i)
	else
		getNoExn = ""
	end if
End Function

Function quitIfErr()
	If Err.number <> 0 Then
		Wscript.Echo "Error # " & CStr(Err.Number) & " " & Err.Description & "  Source: " & Err.Source
		Wscript.Quit Err.Number
	End If
End Function

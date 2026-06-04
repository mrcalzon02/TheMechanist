Option Explicit
Dim shell, fso, root, javaw, cmd
Set shell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
root = fso.GetParentFolderName(WScript.ScriptFullName)
shell.CurrentDirectory = root
javaw = shell.ExpandEnvironmentStrings("%JAVA_HOME%")
If Len(javaw) > 0 And InStr(javaw, "%JAVA_HOME%") = 0 Then
  javaw = fso.BuildPath(javaw, "bin\javaw.exe")
  If Not fso.FileExists(javaw) Then javaw = "javaw.exe"
Else
  javaw = "javaw.exe"
End If
cmd = """" & javaw & """ -Dmechanist.assetRoot=. -Dmechanist.generatedAssetRoot=. -Dmechanist.assetTier=low_32 -Dmechanist.assetResolution=32 -cp ""classes;."" mechanist.TheMechanist"
shell.Run cmd, 0, False

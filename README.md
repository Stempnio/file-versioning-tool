## File Versioning Tool

Simplified version control system. Supports only files. 

FVT class is used to run every command. 

General scheme: java *path_to_fvt* /FVT.java *command* ...

### Commands:

#### init
- initialises FVT system in current directory

#### add *file* -m "optional message"
- adds file given as a command parameter. Takes optional message parameter

#### detach *file* -m "optional message"
- detaches from FVT (but NOT remove files from system) file given as a command parameter. Takes optional message parameter

#### checkout *version*
- restores files to the state of the specific version given as a parameter.

#### commit *file* -m "optional message"
- creates new version in FVT with file given as a parameter. Takes optional message

#### history -last n
- displays history of versions. Takes optional parameter (-last) to display n last versions.

#### version x
- displays details of version x given as parameter. If not specified last version is displayed

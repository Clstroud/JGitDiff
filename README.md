JGitDiff
==============

This is a Java utility designed to take two commits in a (local) repository and create a more readily-digestible
report of the changes that occurred between them. This is specifically designed to only report **3** types of files
as of this writing:
	
	
* .java
* .jsp
* .sql 

This was essentially designed to meet the needs of NC State's CSC-326 (Undergraduate Software Engineering) course, but could be
adapted to other types of projects if desired. The overall goal is to produce a plaintext summary of what files
were changed and, if a Java class, what methods were changed.


Documentation
=============
Every class is fully JavaDoc'd and browsable [here](http://clstroud.github.io/JGitDiff/Docs/)


Usage
======
It's made to be nice and simple, but please view the usage guide [here](https://github.com/Clstroud/JGitDiff/wiki/Usage)

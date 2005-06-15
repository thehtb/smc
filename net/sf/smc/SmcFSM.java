//
// The contents of this file are subject to the Mozilla Public
// License Version 1.1 (the "License"); you may not use this file
// except in compliance with the License. You may obtain a copy of
// the License at http://www.mozilla.org/MPL/
// 
// Software distributed under the License is distributed on an "AS
// IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
// implied. See the License for the specific language governing
// rights and limitations under the License.
// 
// The Original Code is State Machine Compiler (SMC).
// 
// The Initial Developer of the Original Code is Charles W. Rapp.
// Portions created by Charles W. Rapp are
// Copyright (C) 2000 - 2005. Charles W. Rapp.
// All Rights Reserved.
// 
// Contributor(s):
//   Eitan Suez contributed examples/Ant.
//   (Name withheld) contributed the C# code generation and
//   examples/C#.
//   Francois Perrad contributed the Python code generation and
//   examples/Python.
//
// RCS ID
// $Id$
//
// CHANGE LOG
// (See the bottom of this file.)
//

package net.sf.smc;

import java.io.PrintStream;
import java.text.ParseException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

public final class SmcFSM
    extends SmcElement
{
//---------------------------------------------------------------
// Member methods
//

    public SmcFSM(String name)
    {
        // The abstract syntax tree always starts on line 1.
        super (name, 1);

        _startState = "";
        _source = "";
        _context = "";
        _header = "";
        _includeList = (List) new LinkedList();
        _package = null;
        _importList = (List) new LinkedList();
        _declareList = (List) new LinkedList();
        _headerLine = -1;
        _maps = (List) new LinkedList();
    }

    public String getSource()
    {
        return (_source);
    }

    public void setSource(String source)
    {
        _source = source;
        return;
    }

    public int getHeaderLine()
    {
        return (_headerLine);
    }

    public void setHeaderLine(int line_number)
    {
        if (_headerLine < 0)
        {
            _headerLine = line_number;
        }

        return;
    }

    public String getStartState()
    {
        return (_startState);
    }

    public void setStartState(String state)
    {
        _startState = state;
        return;
    }

    public String getContext()
    {
        return (_context);
    }

    public void setContext(String context)
    {
        _context = context;
        return;
    }

    public String getHeader()
    {
        return (_header);
    }

    public void setHeader(String header)
    {
        char c = header.charAt(0);

        // If the header is not enclosed in quotes or <>, then
        // place quotes around the file now.
        if (c != '"' && c != '<')
        {
            _header = "\"" + header.trim() + "\"";
        }
        else
        {
            _header = header.trim();
        }

        _includeList.add(_header);

        return;
    }

    public List getIncludes()
    {
        return (_includeList);
    }

    public void addInclude(String includeFile)
    {
        char c = includeFile.charAt(0);
        String filename;

        // If the header is not enclosed in quotes or <>, then
        // place quotes around the file now.
        if (c != '"' && c != '<')
        {
            filename = "\"" + includeFile.trim() + "\"";
        }
        else
        {
            filename = includeFile.trim();
        }

        _includeList.add(filename);
        return;
    }

    public String getPackage()
    {
        return (_package);
    }

    public void setPackage(String pkg)
    {
        _package = pkg;
        return;
    }

    public List getImports()
    {
        return (_importList);
    }

    public void addImport(String name)
    {
        _importList.add(name);
        return;
    }

    public int getImportCount()
    {
        return (_importList.size());
    }

    public void addDeclare(String name)
    {
        _declareList.add(name);
        return;
    }

    public int getDeclareCount()
    {
        return (_declareList.size());
    }

    public List getDeclarations()
    {
        return (_declareList);
    }

    public SmcMap findMap(String name)
    {
        Iterator mapIt;
        SmcMap map;
        SmcMap retval;

        for (mapIt = _maps.iterator(), retval = null;
             mapIt.hasNext() == true && retval == null;
            )
        {
            map = (SmcMap) mapIt.next();
            if (map.getName().compareTo(name) == 0)
            {
                retval = map;
            }
        }

        return (retval);
    }

    public List getMaps()
    {
        return (_maps);
    }

    public void addMap(SmcMap map)
    {
        _maps.add(map);
        return;
    }

    //-----------------------------------------------------------
    // SmcElement Abstract Methods.
    //

    public void accept(SmcVisitor visitor)
    {
        visitor.visit(this);
    }

    //
    // end of SmcElement Abstract Methods.
    //-----------------------------------------------------------

    public void dump(PrintStream stream)
    {
        Iterator map_it;
        SmcMap map;

        stream.print("Start State: ");
        stream.println(_startState);
        stream.print("     Source:");
        if (_source.length() == 0)
        {
            stream.println(" none.");
        }
        else
        {
            stream.println();
            stream.println(_source);
        }
        stream.print("    Context: ");
        stream.println(_context);

        if (Smc._targetLanguage == Smc.C_PLUS_PLUS)
        {
            Iterator it;

            for (it = _includeList.iterator();
                 it.hasNext() == true;
                )
            {
                stream.print("     Include: ");
                stream.println(((String) it.next()));
            }
        }

        stream.println("       Maps:");
        stream.println();

        for (map_it = _maps.iterator();
             map_it.hasNext() == true;
            )
        {
            map = (SmcMap) map_it.next();
            stream.println(map);
        }

        return;
    }

//---------------------------------------------------------------
// Member data
//

    // The state map's initial state.
    private String _startState;

    // Raw source code appearing at the beginning of the state
    // map source file.
    private String _source;

    // This state map is associated with this class.
    private String _context;

    // Where the associated class is defined.
    private String _header;

    // For C++ only. List of include files. Will be output to
    // the .cpp file in the same order as they appear in the
    // .sm file.
    private List _includeList;

    // This code is placed in this package/namespace.
    private String _package;

    // Place names of imports in this list.
    private List _importList;

    // Place forward declarations in this list.
    private List _declareList;

    // The line where %start, etc. should appear.
    // Used in error messages.
    private int _headerLine;

    // The state maps.
    private List _maps;
}

//
// CHANGE LOG
// $Log$
// Revision 1.1  2005/05/28 19:28:42  cwrapp
// Moved to visitor pattern.
//
// Revision 1.6  2005/02/21 15:35:11  charlesr
// Added Francois Perrad to Contributors section for Python work.
//
// Revision 1.5  2005/02/03 16:44:40  charlesr
// In implementing the Visitor pattern, the generateCode()
// methods have been moved to the appropriate Visitor
// subclasses (e.g. SmcJavaGenerator). This class is now
// extends SmcElement.
//
// Also changed this class name from SmcParseTree to SmcFSM.
//
// Revision 1.4  2004/10/30 16:06:54  charlesr
// Added Graphviz DOT file generation.
//
// Revision 1.3  2004/10/02 19:53:57  charlesr
// Removed "using namespace std" from the generated .cpp file.
// References to std namespace are fully-qualified with "std::".
// Also replaced all string concatenation from print statements.
//
// Revision 1.2  2004/09/06 16:40:49  charlesr
// Added C# support.
//
// Revision 1.1  2004/05/31 13:55:51  charlesr
// Added support for VB.net code generation.
//
// Revision 1.0  2003/12/14 21:04:51  charlesr
// Initial revision
//
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
// Copyright (C) 2005. Charles W. Rapp.
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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Visits the abstract syntax tree emitting a C++ header file.
 * @see SmcElement
 * @see SmcVisitor
 * @see SmcCGenerator
 *
 * @author Francois Perrad
 */

public final class SmcHeaderCGenerator
    extends SmcCodeGenerator
{
//-----------------------------------------------------------------
// Member methods
//

    public SmcHeaderCGenerator(PrintStream source,
                              String srcfileBase)
    {
        super (source, srcfileBase);
    }

    public void visit(SmcFSM fsm)
    {
        String srcfileCaps;
        String packageName = fsm.getPackage();
        String context = fsm.getContext();
        String mapName;
        List transList = (List) new ArrayList();
        String separator;
        List params;
        Iterator it;
        Iterator mapIt;
        Iterator transIt;
        Iterator pit;
        String declaration;
        SmcMap map;
        SmcState state;
        SmcTransition trans;
        SmcParameter param;
        int index;

        // If a package has been specified, 
        if (packageName != null && packageName.length() > 0)
        {
              context = packageName + "_" + context;
        }

        // The first two lines in the header file should be:
        //
        //    #ifndef _H_<source file name>_SM
        //    #define _H_<source file name>_SM
        //
        // where the source file name is all in caps.
        // The last line is:
        //
        //    #endif
        //

        // Make the file name upper case and replace
        // slashes with underscores.
        srcfileCaps =
            _srcfileBase.toUpperCase().replace('\\', '_');
        _source.print("#ifndef _H_");
        _source.print(srcfileCaps);
        _source.println("_SM");
        _source.print("#define _H_");
        _source.print(srcfileCaps);
        _source.println("_SM");

        // Include required standard .h files.
        _source.println();
        _source.println("#include <statemap.h>");

        _source.println();

        // Do user-specified forward declarations now.
        for (it = fsm.getDeclarations().iterator();
             it.hasNext() == true;
            )
        {
            declaration = (String) it.next();
            _source.print(declaration);

            // Add a semicolon if the user did not use one.
            if (declaration.endsWith(";") == false)
            {
                _source.print(";");
            }

            _source.println();
        }

        // Forward declare the application class.
        _source.println();
        _source.print("struct ");
        _source.print(context);
        _source.println(";");
        _source.print("struct ");
        _source.print(context);
        _source.println("Context;");

        // Declare user's base state class.
        _source.println();
        _source.print("struct ");
        _source.print(context);
        _source.println("State");
        _source.println("{");

        // Add the default Entry() and Exit() definitions.
        _source.print("    void(*Entry)(struct ");
        _source.print(context);
        _source.println("Context*);");
        _source.print("    void(*Exit)(struct ");
        _source.print(context);
        _source.println("Context*);");
        _source.println();

        // Print out the default definitions for all the
        // transitions. First, get the transitions list.
        for (mapIt = fsm.getMaps().iterator();
             mapIt.hasNext() == true;
            )
        {
            map = (SmcMap) mapIt.next();

            // Merge the new transitions into the current set.
            transList =
                    Smc.merge(
                        map.getTransitions(),
                        transList,
                        new Comparator() {
                            public int compare(Object o1,
                                               Object o2)
                            {
                                return (
                                    ((SmcTransition)
                                     o1).compareTo(
                                         (SmcTransition) o2));
                            }
                        });
        }

        // Output the global transition declarations.
        for (transIt = transList.iterator();
             transIt.hasNext() == true;
            )
        {
            trans = (SmcTransition) transIt.next();

            // Don't output the default state here.
            if (trans.getName().equals("Default") == false)
            {
                _source.print("    void(*");
                _source.print(trans.getName());
                _source.print(")(struct ");
                _source.print(context);
                _source.print("Context*");

                params = trans.getParameters();
                for (pit = params.iterator();
                     pit.hasNext() == true;
                    )
                {
                    param = (SmcParameter) pit.next();

                    _source.print(", ");
                    _source.print(param.getType());
                }

                _source.println(");");
            }
        }
        _source.println();
        _source.print("    void(*Default)(struct ");
        _source.print(context);
        _source.println("Context*);");

        _source.println();
        _source.println("    STATE_MEMBERS");

        // The base class has been defined.
        _source.println("};");
        _source.println();

        // Generate the map classes. The maps will, in turn,
        // generate the state classes.
        for (mapIt = fsm.getMaps().iterator();
             mapIt.hasNext() == true;
            )
        {
            ((SmcMap) mapIt.next()).accept(this);
        }

        // Generate the FSM context class.
        _source.println();
        _source.print("struct ");
        _source.print(context);
        _source.println("Context");
        _source.println("{");
        _source.print("    FSM_MEMBERS(");
        _source.print(context);
        _source.println(")");
        _source.print("    struct ");
        _source.print(context);
        _source.println(" *_owner;");

        // Put the closing brace on the context class.
        _source.println("};");
        _source.println();

        // Constructor
        _source.print("extern void ");
        _source.print(context);
        _source.print("Context_Init");
        _source.print("(struct ");
        _source.print(context);
        _source.print("Context*, struct ");
        _source.print(context);
        _source.println("*);");

        // Generate a method for every transition in every map
        // *except* the default transition.
        for (transIt = transList.iterator();
             transIt.hasNext() == true;
            )
        {
            trans = (SmcTransition) transIt.next();
            if (trans.getName().equals("Default") == false)
            {
                _source.print("extern void ");
                _source.print(context);
                _source.print("Context_");
                _source.print(trans.getName());
                _source.print("(struct ");
                _source.print(context);
                _source.print("Context*");

                params = trans.getParameters();
                for (pit = params.iterator();
                     pit.hasNext() == true;
                    )
                {
                    param = (SmcParameter) pit.next();

                    _source.print(", ");
                    _source.print(param.getType());
                }
                _source.println(");");
            }
        }

        _source.println();
        _source.println("#endif");

        return;
    }

    // Generate the map class declaration
    public void visit(SmcMap map)
    {
        String packageName = map.getFSM().getPackage();
        String context = map.getFSM().getContext();
        String mapName = map.getName();
        Iterator it;
        SmcState state;

        // If a package has been specified, 
        if (packageName != null && packageName.length() > 0)
        {
              context = packageName + "_" + context;
        }

        for (it = map.getStates().iterator();
             it.hasNext() == true;
            )
        {
            state = (SmcState) it.next();
            _source.print("extern const struct ");
            _source.print(context);
            _source.print("State ");
            if (packageName != null && packageName.length() > 0)
            {
                _source.print(packageName);
                _source.print("_");
            }
            _source.print(mapName);
            _source.print("_");
            _source.print(state.getClassName());
            _source.println(";");    
        }

        return;
    }

//-----------------------------------------------------------------
// Member data
//
}

//
// CHANGE LOG
// $Log$
// Revision 1.1  2005/06/16 18:11:01  fperrad
// Added C, Perl & Ruby generators.
//
//
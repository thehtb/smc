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
// Port to Python by Francois Perrad, francois.perrad@gadz.org
// Copyright 2004, Francois Perrad.
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Visits the abstract syntax tree, emitting Python code.
 * @see SmcElement
 * @see SmcCodeGenerator
 * @see SmcVisitor
 *
 * @author Francois Perrad
 */

public final class SmcPythonGenerator
    extends SmcCodeGenerator
{
//---------------------------------------------------------------
// Member methods
//

    public SmcPythonGenerator(PrintStream source,
                              String srcfileBase)
    {
        super (source, srcfileBase);
    }

    public void visit(SmcFSM fsm)
    {
        String context = fsm.getContext();
        String rawSource = fsm.getSource();
        String startState = fsm.getStartState();
        String pythonState;
        List maps = fsm.getMaps();
        List transitions;
        List params;
        SmcMap map;
        SmcTransition trans;
        String transName;
        int index;
        Iterator it;
        Iterator it2;

        // Dump out the raw source code, if any.
        if (rawSource != null && rawSource.length () > 0)
        {
            _source.println(rawSource);
            _source.println();
        }

        _source.println("import statemap");
        _source.println();

        // Do user-specified imports now.
        for (it = fsm.getImports().iterator();
             it.hasNext() == true;
            )
        {
            _source.print("import ");
            _source.println(it.next());
        }

        // Declare the inner state class.
        _source.println();
        _source.print("class ");
        _source.print(context);
        _source.println("State(statemap.State):");

        _source.println();
        _source.println("    def Entry(self, fsm):");
        _source.println("        pass");
        _source.println();
        _source.println("    def Exit(self, fsm):");
        _source.println("        pass");
        _source.println();

        // Get the transition list.
        // Generate the default transition definitions.
        transitions = fsm.getTransitions();
        for (it = transitions.iterator(); it.hasNext() == true;)
        {
            trans = (SmcTransition) it.next();
            params = trans.getParameters();

            // Don't generate the Default transition here.
            if (trans.getName().equals("Default") == false)
            {
                _source.print("    def ");
                _source.print(trans.getName());
                _source.print("(self, fsm");

                for (it2 = params.iterator();
                     it2.hasNext() == true;
                    )
                {
                    _source.print(", ");
                    ((SmcParameter) it2.next()).accept(this);
                }

                _source.println("):");

                // If this method is reached, that means that this
                // transition was passed to a state which does not
                // define the transition. Call the state's default
                // transition method.
                _source.println("        self.Default(fsm)");
                _source.println();
            }
        }

        // Generate the overall Default transition for all maps.
        _source.println("    def Default(self, fsm):");

        if (Smc.isDebug() == true)
        {
            _source.println(
                "        if fsm.getDebugFlag() == True:");
            _source.println(
                "            fsm.getDebugStream().write('TRANSITION   : Default\\n')");
        }

        _source.println(
            "        msg = \"\\n\\tState: %s\\n\\tTransition: %s\" % (");
        _source.println(
            "            fsm.getState().getName(), fsm.getTransition())");
        _source.println(
            "        raise TransitionUndefinedException, msg");

        // Have each map print out its source code now.
        for (it = maps.iterator();  it.hasNext() == true;)
        {
            ((SmcMap) it.next()).accept(this);
        }

        // The context class contains all the state classes as
        // inner classes, so generate the context first rather
        // than last.
        _source.println();
        _source.print("class ");
        _source.print(context);
        _source.println("_sm(statemap.FSMContext):");
        _source.println();

        // Generate the context class' constructor.
        _source.println("    def __init__(self, owner):");
        _source.println(
            "        statemap.FSMContext.__init__(self)");
        _source.println("        self._owner = owner");

        // The state name "map::state" must be changed to
        // "map.state".
        if ((index = startState.indexOf("::")) >= 0)
        {
            pythonState =
                startState.substring(0, index) +
                "." +
                startState.substring(index + 2);
        }
        else
        {
            pythonState = startState;
        }

        _source.print("        self.setState(");
        _source.print(pythonState);
        _source.println(")");

        // Execute the start state's entry actions.
        _source.print("        ");
        _source.print(pythonState);
        _source.println(".Entry(self)");

        _source.println();

        // Generate the transition methods.
        for (it = transitions.iterator(); it.hasNext() == true;)
        {
            trans = (SmcTransition) it.next();
            transName = trans.getName();
            params = trans.getParameters();

            if (transName.equals("Default") == false)
            {
                _source.print("    def ");
                _source.print(transName);
                _source.print("(self");
                if (params.size() != 0)
                {
                    _source.print(", *arglist");    
                }
                _source.println("):");

                // Save away the transition name in case it is
                // need in an UndefinedTransitionException.
                _source.print("        self._transition = '");
                _source.print(transName);
                _source.println("'");

                _source.print("        self.getState().");
                _source.print(transName);
                _source.print("(self");

                if (params.size() != 0)
                {
                    _source.print(", *arglist");    
                }
                _source.println(")");
                _source.println(
                    "        self._transition = None");

                _source.println();
            }
        }

        // getState() method.
        _source.println("    def getState(self):");
        _source.println("        if self._state == None:");
        _source.println(
            "            raise statemap.StateUndefinedException");
        _source.println("        return self._state");
        _source.println();

        // getOwner() method.
        _source.println("    def getOwner(self):");
        _source.println("        return self._owner");
        _source.println();

        return;
    }

    public void visit(SmcMap map)
    {
        List definedDefaultTransitions;
        SmcState defaultState = map.getDefaultState();
        String context = map.getFSM().getContext();
        String mapName = map.getName();
        List states = map.getStates();
        Iterator it;
        SmcState state;
        boolean needPass = true;

        // Initialize the default transition list to all the
        // default state's transitions.
        if (defaultState != null)
        {
            definedDefaultTransitions =
                    defaultState.getTransitions();
        }
        else
        {
            definedDefaultTransitions = (List) new ArrayList();
        }

        // Declare the map default state class.
        _source.println();
        _source.print("class ");
        _source.print(mapName);
        _source.print("_Default(");
        _source.print(context);
        _source.println("State):");

        // Declare the user-defined default transitions first.
        for (it = definedDefaultTransitions.iterator();
             it.hasNext() == true;
            )
        {
            needPass = false;
            ((SmcTransition) it.next()).accept(this);
        }

        if (needPass == true) 
        {
            _source.println("    pass");
        }
        
        // Have each state now generate its code. Each state
        // class is an inner class.
        for (it = states.iterator(); it.hasNext() == true;)
        {
            ((SmcState) it.next()).accept(this);
        }

        // Declare and initialize the map class. 
        _source.println();
        _source.print("class ");
        _source.print(mapName);
        _source.println(':');
        _source.println();

        for (it = states.iterator(); it.hasNext() == true;)
        {
            state = (SmcState) it.next();

            _source.print("    ");
            _source.print(state.getInstanceName());
            _source.print(" = ");
            _source.print(mapName);
            _source.print('_');
            _source.print(state.getClassName());
            _source.print("('");
            _source.print(mapName);
            _source.print('.');
            _source.print(state.getClassName());
            _source.print("', ");
            _source.print(map.getNextStateId());
            _source.println(")");
        }

        // Instantiate a default state as well.
        _source.print("    Default = ");
        _source.print(mapName);
        _source.print("_Default('");
        _source.print(mapName);
        _source.println(".Default', -1)");

        return;
    }

    public void visit(SmcState state)
    {
        SmcMap map = state.getMap();
        String mapName = map.getName();
        String stateName = state.getClassName();
        List actions;
        String indent2;
        Iterator it;
        boolean needPass = true;

        // Declare the inner state class.
        _source.println();
        _source.print("class ");
        _source.print(mapName);
        _source.print('_');
        _source.print(stateName);
        _source.print("(");
        _source.print(mapName);
        _source.println("_Default):");

        // Add the Entry() and Exit() member functions if this
        // state defines them.
        actions = state.getEntryActions();
        if (actions != null && actions.size() > 0)
        {
            needPass = false;

            _source.println();
            _source.println("    def Entry(self, fsm):");

            // Declare the "ctxt" local variable.
            _source.println("        ctxt = fsm.getOwner()");

            // Generate the actions associated with this code.
            indent2 = _indent;
            _indent = "        ";
            for (it = actions.iterator(); it.hasNext() == true;)
            {
                ((SmcAction) it.next()).accept(this);
            }
            _indent = indent2;
        }

        actions = state.getExitActions();
        if (actions != null && actions.size() > 0)
        {
            needPass = false;

            _source.println();
            _source.println("    def Exit(self, fsm):");

            // Declare the "ctxt" local variable.
            _source.println("        ctxt = fsm.getOwner()");

            // Generate the actions associated with this code.
            indent2 = _indent;
            _indent = "        ";
            for (it = actions.iterator(); it.hasNext() == true;)
            {
                ((SmcAction) it.next()).accept(this);
            }
            _indent = indent2;
        }

        // Have each transition generate its code.
        for (it = state.getTransitions().iterator();
             it.hasNext() == true;
            )
        {
            needPass = false;

            ((SmcTransition) it.next()).accept(this);
        }

        if (needPass == true)
        {
            _source.println("    pass");
        }

        return;
    }

    public void visit(SmcTransition transition)
    {
        SmcState state = transition.getState();
        SmcMap map = state.getMap();
        String mapName = map.getName();
        String stateName = state.getClassName();
        String transName = transition.getName();
        List parameters = transition.getParameters();
        List guards = transition.getGuards();
        boolean nullCondition = false;
        Iterator it;
        SmcGuard guard;
        SmcParameter param;

        _source.println();
        _source.print("    def ");
        _source.print(transName);
        _source.print("(self, fsm");

        // Add user-defined parameters.
        for (it = parameters.iterator(); it.hasNext() == true;)
        {
            _source.print(", ");
            ((SmcParameter) it.next()).accept(this);
        }
        _source.println("):");

        // All transitions have a "ctxt" local variable.
        // 8/14/2003:
        // Do this only if there are any transition actions or
        // guard conditions which reference it.
        if (transition.hasCtxtReference() == true)
        {
            _source.println("        ctxt = fsm.getOwner()");
        }

        // Output transition to debug stream.
        if (Smc.isDebug() == true)
        {
            String sep;

            _source.println(
                "        if fsm.getDebugFlag() == True:");
            _source.print(
                "            fsm.getDebugStream().write(\"TRANSITION   : ");
            _source.print(mapName);
            _source.print(".");
            _source.print(stateName);
            _source.print(".");
            _source.print(transName);

            _source.print("(");
            for (it = parameters.iterator(), sep = "";
                 it.hasNext() == true;
                 sep = ", ")
            {
                _source.print(sep);
                ((SmcParameter) it.next()).accept(this);
            }
            _source.print(")");

            _source.println("\\n\")");
            _source.println();
        }

        // Loop through the guards and print each one.
        _indent = "        ";
        for (it = guards.iterator(),
                  _guardIndex = 0,
                  _guardCount = guards.size();
             it.hasNext() == true;
             ++_guardIndex)
        {
            guard = (SmcGuard) it.next();

            // Count up the guards with no condition.
            if (guard.getCondition().length() == 0)
            {
                nullCondition = true;
            }

            guard.accept(this);
        }

        // If all guards have a condition, then create a final
        // "else" clause which passes control to the default
        // transition. Pass all arguments into the default
        // transition.
        if (_guardIndex > 0 && nullCondition == false)
        {
            _source.println("        else:");

            // Call the super class' transition method using
            // the class name.
            _source.print("            ");
            _source.print(mapName);
            _source.print("_Default.");
            _source.print(transName);
            _source.print("(self, fsm");

            for (it = parameters.iterator();
                 it.hasNext() == true;
                )
            {
                _source.print(", ");
                ((SmcParameter) it.next()).accept(this);
            }

            _source.println(")");
            _source.print("        ");
        }
        // Need to add a final newline after a multiguard block.
        else if (_guardCount > 1)
        {
            _source.println();
        }

        return;
    }

    public void visit(SmcGuard guard)
    {
        SmcTransition transition = guard.getTransition();
        SmcState state = transition.getState();
        SmcMap map = state.getMap();
        String context = map.getFSM().getContext();
        String mapName = map.getName();
        String stateName = state.getClassName();
        int transType = guard.getTransType();
        boolean defaultFlag =
            stateName.equalsIgnoreCase("Default");
        boolean loopbackFlag = false;
        String indent2;
        String indent3;
        String indent4;
        String endStateName = guard.getEndState();
        String fqEndStateName = "";
        String pushStateName = guard.getPushState();
        String condition = guard.getCondition();
        List actions = guard.getActions();

        // If this guard's end state is not of the form
        // "map::state", then prepend the map name to the
        // state name.
        // DON'T DO THIS IF THIS IS A POP TRANSITION!
        // The "state" is actually a transition name.
        if (transType != Smc.TRANS_POP &&
            endStateName.length () > 0 &&
            endStateName.equals(NIL_STATE) == false)
        {
            endStateName = scopeStateName(endStateName, mapName);
        }

        // Qualify the state and push state names as well.
        stateName = scopeStateName(stateName, mapName);
        pushStateName = scopeStateName(pushStateName, mapName);

        loopbackFlag =
            isLoopback(transType, stateName, endStateName);

        // The guard code generation is a bit tricky. The first
        // question is how many guards are there? If there are
        // more than one, then we will need to generate the
        // proper "if-then-else" code.
        if (_guardCount > 1)
        {
            indent2 = _indent + "    ";

            // There are multiple guards. Is this the first guard?
            if (_guardIndex == 0 && condition.length() > 0)
            {
                // Yes, this is the first. This means an "if"
                // should be used.
                _source.print(_indent);
                _source.print("if ");
                _source.print(condition);
                _source.println(" :");
            }
            else if (condition.length() > 0)
            {
                // No, this is not the first transition but it
                // does have a condition. Use an "else if".
                _source.print(_indent);
                _source.print("elif ");
                _source.print(condition);
                _source.println(" :");
            }
            else
            {
                // This is not the first transition and it has
                // no condition.
                _source.print(_indent);
                _source.println("else:");
            }
        }
        // There is only one guard. Does this guard have
        // a condition?
        else if (condition.length() == 0)
        {
            // No. This is a plain, old. vanilla transition.
            indent2 = _indent;
        }
        else
        {
            // Yes there is a condition.
            indent2 = _indent + "        ";

            _source.print(_indent);
            _source.print("    if ");
            _source.print(condition);
            _source.println(" :");
        }

        // Now that the necessary conditions are in place, it's
        // time to dump out the transition's actions. First, do
        // the proper handling of the state change. If this
        // transition has no actions, then set the end state
        // immediately. Otherwise, unset the current state so
        // that if an action tries to issue a transition, it will
        // fail.
        if (actions.size() == 0 && endStateName.length() != 0)
        {
            fqEndStateName = endStateName;
        }
        else if (actions.size() > 0)
        {
            // Save away the current state if this is a loopback
            // transition. Storing current state allows the
            // current state to be cleared before any actions are
            // executed. Remember: actions are not allowed to
            // issue transitions and clearing the current state
            // prevents them from doing do.
            if (loopbackFlag == true)
            {
                fqEndStateName = "endState";

                _source.print(indent2);
                _source.print(fqEndStateName);
                _source.println(" = fsm.getState()");
            }
            else
            {
                fqEndStateName = endStateName;
            }
        }

        // Decide if runtime loopback checking must be done.
        if (defaultFlag == true &&
            transType != Smc.TRANS_POP &&
            loopbackFlag == false)
        {
            _source.print(_indent);
            _source.print(
                "loopbackFlag = fsm.getState().getName() == ");
            _source.print(fqEndStateName);
            _source.println(".getName()");
        }

        // Dump out the exit actions - but only for the first guard.
        // v. 1.0, beta 3: Not any more. The exit actions are
        // executed only if 1) this is a standard, non-loopback
        // transition or a pop transition.
        if (transType == Smc.TRANS_POP || loopbackFlag == false)
        {
            indent4 = indent2;

            // If this is a non-loopback, generic transition,
            // do runtime loopback checking.
            if (transType == Smc.TRANS_SET &&
                defaultFlag == true)
            {
                indent4 = indent2 + "    ";

                _source.print(indent2);
                _source.println("if loopbackFlag == False:");
            }

            _source.print(indent4);
            _source.println("fsm.getState().Exit(fsm)");
        }

        // Dump out this transition's actions.
        if (actions.size() == 0)
        {
            List entryActions = state.getEntryActions();
            List exitActions = state.getExitActions();

            if (condition.length() > 0)
            {
                _source.print(indent2);
                _source.println("# No actions.");
            }
            // If there are:
            // 1. No entry actions,
            // 2. No exit actions,
            // 3. Only one guard,
            // 4. No condition,
            // 5. No actions,
            // 6. Not a loopback or pop transition and
            // 7. No debug code being generated.
            // then give this transition a pass.
            else if (_guardCount == 1 &&
                     (entryActions == null ||
                      entryActions.isEmpty() == true) &&
                     (exitActions == null ||
                      exitActions.isEmpty() == true) &&
                     transType != Smc.TRANS_POP &&
                     loopbackFlag == true &&
                     Smc.isDebug() == false)
            {
                _source.print(indent2);
                _source.println("pass");
            }

            indent3 = indent2;
        }
        else
        {
            Iterator it;

            // Now that we are in the transition, clear the
            // current state.
            _source.print(indent2);
            _source.println("fsm.clearState()");

            // v. 2.0.0: Place the actions inside a try/finally
            // block. This way the state will be set before an
            // exception leaves the transition method.
            // v. 2.2.0: Check if the user has turned off this
            // feature first.
            if (Smc.isNoCatch() == false)
            {
                _source.print(indent2);
                _source.println("try:");

                indent3 = indent2 + "    ";
            }
            else
            {
                indent3 = indent2;
            }

            indent4 = _indent;
            _indent = indent3;

            for (it = actions.iterator(); it.hasNext() == true;)
            {
                ((SmcAction) it.next()).accept(this);
            }

            _indent = indent4;

            // v. 2.2.0: Check if the user has turned off this
            // feature first.
            if (Smc.isNoCatch() == false)
            {
                _source.print(indent2);
                _source.println("finally:");
            }
        }

        // Print the setState() call, if necessary. Do NOT
        // generate the set state it:
        // 1. The transition has no actions AND is a loopback OR
        // 2. This is a push or pop transition.
        if (transType == Smc.TRANS_SET &&
            (actions.size() > 0 || loopbackFlag == false))
        {
            _source.print(indent3);
            _source.print("fsm.setState(");
            _source.print(fqEndStateName);
            _source.println(")");
        }
        else if (transType == Smc.TRANS_PUSH)
        {
            // Set the next state so this it can be pushed
            // onto the state stack. But only do so if a clear
            // state was done.
            if (loopbackFlag == false || actions.size() > 0)
            {
                _source.print(indent3);
                _source.print("fsm.setState(");
                _source.print(fqEndStateName);
                _source.println(")");
            }

            // Before doing the push, execute the end state's
            // entry actions (if any) if this is not a loopback.
            if (loopbackFlag == false)
            {
                if (defaultFlag == true)
                {
                    indent4 = indent3 + "    ";
                    _source.print(indent3);
                    _source.println("if loopbackFlag == False:");
                }
                else
                {
                    indent4 = indent3;
                }

                _source.print(indent4);
                _source.println("fsm.getState().Entry(fsm)");
            }

            _source.print(indent3);
            _source.print("fsm.pushState(");
            _source.print(pushStateName);
            _source.println(")");
        }
        else if (transType == Smc.TRANS_POP)
        {
            _source.print(indent3);
            _source.println("fsm.popState()");
        }

        // Perform the new state's enty actions.
        // v. 1.0, beta 3: Not any more. The entry actions are
        // executed only if 1) this is a standard, non-loopback
        // transition or a push transition.
        if ((transType == Smc.TRANS_SET &&
             loopbackFlag == false) ||
             transType == Smc.TRANS_PUSH)
        {
            indent4 = indent3;

            // If this is a non-loopback, generic transition,
            // do runtime loopback checking.
            if (transType == Smc.TRANS_SET &&
                defaultFlag == true)
            {
                indent4 = indent3 + "    ";

                _source.print(indent3);
                _source.println("if loopbackFlag == False:");
            }

            _source.print(indent4);
            _source.println("fsm.getState().Entry(fsm)");
        }

        // If there is a transition associated with the pop, then
        // issue that transition here.
        if (transType == Smc.TRANS_POP &&
            endStateName.equals(NIL_STATE) == false &&
            endStateName.length() > 0)
        {
            String popArgs = guard.getPopArgs();

            _source.print(indent2);
            _source.print("fsm.");
            _source.print(endStateName);
            _source.print("(");

            // Output any and all pop arguments.
            if (popArgs.length() > 0)
            {
                _source.print(popArgs);
                _source.println();
                _source.print(indent2);
                _source.println(")");
            }
            else 
            {
                _source.println(")");
            }
        }

        return;
    }

    public void visit(SmcAction action)
    {
        String name = action.getName();
        Iterator it;
        String sep;

        // Need to distinguish between FSMContext actions and
        // application class actions. If the action is
        // "emptyStateStack", then pass it to the context.
        // Otherwise, let the application class handle it.
        _source.print(_indent);
        if (name.equals("emptyStateStack") == true)
        {
            _source.print("fsm.");
        }
        else
        {
            _source.print("ctxt.");
        }
        _source.print(name);
        _source.print("(");

        for (it = action.getArguments().iterator(), sep = "";
             it.hasNext() == true;
             sep = ", ")
        {
            _source.print(sep);
            _source.print((String) it.next());
        }

        _source.println(")");

        return;
    }

    public void visit(SmcParameter parameter)
    {
        _source.print(parameter.getName());
        return;
    }

//---------------------------------------------------------------
// Member data
//
}

//
// CHANGE LOG
// $Log$
// Revision 1.3  2005/11/07 19:34:54  cwrapp
// Changes in release 4.3.0:
// New features:
//
// + Added -reflect option for Java, C#, VB.Net and Tcl code
//   generation. When used, allows applications to query a state
//   about its supported transitions. Returns a list of transition
//   names. This feature is useful to GUI developers who want to
//   enable/disable features based on the current state. See
//   Programmer's Manual section 11: On Reflection for more
//   information.
//
// + Updated LICENSE.txt with a missing final paragraph which allows
//   MPL 1.1 covered code to work with the GNU GPL.
//
// + Added a Maven plug-in and an ant task to a new tools directory.
//   Added Eiten Suez's SMC tutorial (in PDF) to a new docs
//   directory.
//
// Fixed the following bugs:
//
// + (GraphViz) DOT file generation did not properly escape
//   double quotes appearing in transition guards. This has been
//   corrected.
//
// + A note: the SMC FAQ incorrectly stated that C/C++ generated
//   code is thread safe. This is wrong. C/C++ generated is
//   certainly *not* thread safe. Multi-threaded C/C++ applications
//   are required to synchronize access to the FSM to allow for
//   correct performance.
//
// + (Java) The generated getState() method is now public.
//
// Revision 1.2  2005/06/08 11:09:15  cwrapp
// + Updated Python code generator to place "pass" in methods with empty
//   bodies.
// + Corrected FSM errors in Python example 7.
// + Removed unnecessary includes from C++ examples.
// + Corrected errors in top-level makefile's distribution build.
//
// Revision 1.1  2005/05/28 19:28:42  cwrapp
// Moved to visitor pattern.
//
// Revision 1.0  2005/02/21 15:40:31  charlesr
// Initial revision
//

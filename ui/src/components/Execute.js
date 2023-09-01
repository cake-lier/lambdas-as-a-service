import { useState, useCallback } from "react";
import _ from "lodash";
import { InputTextarea } from 'primereact/inputtextarea';
import { InputText } from 'primereact/inputtext';
import { Dialog } from "primereact/dialog";
import { Button } from "primereact/button";
import { Card } from 'primereact/card';
import PageHeader from "./PageHeader";

export default function Execute({ userState, logout, deploy, execute, setLastExecution, lastExecution, lastDeployment }) {
    const [executable, setExecutable] = useState("");
    const [args, setArgs] = useState("");
    const [open, setOpen] = useState(lastExecution !== null);
    const [executing, setExecuting] = useState(false);
    const doExecute = useCallback(() => {
        execute(executable, args.split(/\r?\n/));
        setExecuting(true);
    }, [execute, executable, args, setExecuting]);
    return (
        <>
            <Dialog
                dismissableMask={ !executing || lastExecution !== null }
                draggable={ false }
                resizable={ false }
                footer={ <div className="grid"><br/><br/></div> }
                visible={ open }
                onHide={ () => { setOpen(false); setExecuting(false); setLastExecution(null); setExecutable(""); setArgs("") } }
            >
                {
                    lastExecution === null
                    ? <div className="grid">
                        <div className="col-12">
                            <h1 className="font-bold text-xl">Execute</h1>
                        </div>
                        <InputTextarea
                            id="args"
                            value={ args }
                            onChange={ e => setArgs(e.target.value) }
                            rows={ 5 }
                            cols={ 30 }
                            className="w-full mt-5"
                            disabled={ executing }
                            placeholder="Insert one argument per row"
                        />
                        <Button
                           className="w-full mt-5"
                           label="Execute"
                           loading={ executing }
                           onClick={ doExecute }
                        />
                    </div>
                    : <div>
                        <div>
                            <h1 className="font-bold text-xl">Execution results</h1>
                        </div>
                        <div className="w-full mt-3">
                            <label htmlFor="exitCode">Exit code</label>
                            <InputText
                                id="exitCode"
                                className="w-full mt-2"
                                value={ lastExecution.exitCode }
                            />
                        </div>
                        <div className="w-full mt-3">
                            <label htmlFor="stdout">Standard output</label>
                            <InputTextarea
                                id="stdout"
                                className="w-full mt-2"
                                value={ lastExecution.stdout }
                                rows={ 5 }
                                cols={ 30 }
                            />
                        </div>
                        <div className="w-full mt-3">
                            <label htmlFor="stderr">Standard error</label>
                            <InputTextarea
                                id="stderr"
                                className="w-full mt-2"
                                value={ lastExecution.stderr }
                                rows={ 5 }
                                cols={ 30 }
                            />
                        </div>
                    </div>
                }
            </Dialog>
            <div className="grid h-screen">
                <div className="mx-0 p-0 flex-column flex-1 hidden md:flex h-screen">
                    <PageHeader
                        logout={ logout }
                        isResponsive={ false }
                        deploy={ deploy }
                        lastDeployment={ lastDeployment }
                    />
                    <div className="grid justify-content-center align-content-start overflow-y-auto h-full">
                        {
                            _.range(0, userState.executables.length).map(index =>
                                <Card
                                    key={index}
                                    className="w-10rem m-3"
                                    style={{ overflowWrap: "break-word" }}
                                >
                                    <img
                                        src={ `executable.png` }
                                        alt={ userState.executables[index].name }
                                        className="w-full"
                                    />
                                    <p className="m-0 text-md text-center font-semibold">{ userState.executables[index].name }</p>
                                    <Button
                                        className="w-full mt-3"
                                        label="Execute"
                                        onClick={ () => { setExecutable(userState.executables[index].id); setOpen(true) } }
                                    />
                                </Card>
                            )
                        }
                    </div>
                </div>
                <div className="mx-0 p-0 flex flex-column w-full flex-1 md:hidden">
                    <PageHeader
                        logout={ logout }
                        isResponsive={ true }
                        deploy={ deploy }
                        lastDeployment={ lastDeployment }
                    />
                    <div className="grid flex-grow-1 justify-content-center align-content-start">
                        {
                            _.range(0, userState.executables.length).map(index =>
                                <Card key={index} className="w-10rem m-1">
                                    <img
                                        src={ `executable.png` }
                                        alt={ userState.executables[index].name }
                                        className="w-full"
                                    />
                                    <p className="m-0 text-md text-center font-semibold" style={{ overflowWrap: "break-word" }}>
                                        { userState.executables[index].name }
                                    </p>
                                    <Button
                                        className="w-full mt-3"
                                        label="Execute"
                                        onClick={ () => { setExecutable(userState.executables[index]); setOpen(true) } }
                                    />
                                </Card>
                            )
                        }
                    </div>
                </div>
            </div>
        </>
    );
}
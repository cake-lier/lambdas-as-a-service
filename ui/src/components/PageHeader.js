import { useCallback, useState, useRef } from "react";
import { Button } from 'primereact/button';
import { Divider } from "primereact/divider";
import { InputText } from "primereact/inputtext";
import { FileUpload } from 'primereact/fileupload';
import { Dialog } from "primereact/dialog";

export default function PageHeader({ logout, isResponsive, deploy, lastDeployment }) {
    const fileUploader = useRef();
    const [name, setName] = useState("");
    const [file, setFile] = useState(null);
    const [open, setOpen] = useState(false);
    const doDeploy = useCallback(e => {
        e.preventDefault();
        deploy(name, file);
    }, [name, file, deploy]);
    return (
        <>
            <Dialog
                dismissableMask={ true }
                draggable={ false }
                resizable={ false }
                visible={ open }
                onHide={ () => { setOpen(false); setName(""); setFile(null); fileUploader.current.clear(); } }
            >
                <div className="grid">
                    <div className="col-12">
                        <h1 className="font-bold text-xl">Deploy</h1>
                    </div>
                    <div className="col-12 mt-3">
                        <form onSubmit={ doDeploy }>
                            <span className="p-float-label p-input-icon-right w-full">
                                <InputText
                                    id="name"
                                    name="name"
                                    className="w-full"
                                    value={ name }
                                    onChange={ e => setName(e.target.value) }
                                    disabled={ lastDeployment !== null }
                                />
                                <label htmlFor="name">Name</label>
                            </span>
                            <FileUpload
                                mode="basic"
                                auto
                                customUpload
                                className="w-full mt-2"
                                uploadHandler={ e => setFile(e.files[0]) }
                                ref={ fileUploader }
                                chooseLabel="Choose the executable file"
                                disabled={ file !== null || lastDeployment !== null }
                            />
                            <Button
                                className="w-full mt-2"
                                icon="pi pi-trash"
                                label="Reset selected file"
                                type="button"
                                onClick={ () => { setFile(null); fileUploader.current.clear(); } }
                                disabled={ file === null || lastDeployment !== null }
                            />
                            <Button
                                type="submit"
                                className="w-full mt-5"
                                label="Deploy"
                                loading={ lastDeployment !== null }
                            />
                        </form>
                    </div>
                </div>
            </Dialog>
            {
                isResponsive
                ? <div className="grid sticky top-0 z-1 mb-3" style={{ backgroundColor: "var(--surface-card)" }}>
                    <div className="col-5 col-offset-1 flex flex-columns justify-content-right align-items-center">
                        <img className="h-4rem" src="logo192.png" alt="App logo" />
                        <div className="w-max m-0 p-2 py-3 flex flex-column justify-content-center">
                            <h3 className="text-3xl font-semibold flex align-items-center">LaaS</h3>
                        </div>
                    </div>
                    <div className="col-5 grid flex flex-columns justify-content-between align-items-center">
                        <Button
                            className="col-12"
                            label="Add file"
                            onClick={ () => setOpen(true) }
                        />
                        <Button
                            className="col-12 mt-1"
                            label="Logout"
                            onClick={ logout }
                        />
                    </div>
                </div>
                : <div className="grid mb-3" style={{ backgroundColor: "var(--surface-card)" }}>
                    <div className="col-5 lg:col-7 col-offset-1 flex flex-row">
                        <img className="h-4rem" src="logo192.png" alt="App logo" />
                        <div className="w-max ml-3 flex flex-column justify-content-center">
                            <h3 className="text-3xl font-semibold flex align-items-center">Lambdas-as-a-Service</h3>
                        </div>
                    </div>
                    <div className="col-5 lg:col-3 flex justify-content-between align-items-center">
                        <Button
                            label="Add file"
                            onClick={ () => setOpen(true) }
                        />
                        <Button
                            label="Logout"
                            onClick={ logout }
                        />
                    </div>
                </div>
            }
        </>
    );
};
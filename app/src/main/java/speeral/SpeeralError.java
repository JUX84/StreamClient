// **********************************************************************
//
// Copyright (c) 2003-2013 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************
//
// Ice version 3.5.1
//
// <auto-generated>
//
// Generated from file `speer_serv.ice'
//
// Warning: do not edit this file.
//
// </auto-generated>
//

package speeral;

public class SpeeralError extends Ice.UserException
{
    public SpeeralError()
    {
    }

    public SpeeralError(Throwable __cause)
    {
        super(__cause);
    }

    public SpeeralError(String reason)
    {
        this.reason = reason;
    }

    public SpeeralError(String reason, Throwable __cause)
    {
        super(__cause);
        this.reason = reason;
    }

    public String
    ice_name()
    {
        return "speeral::SpeeralError";
    }

    public String reason;

    protected void
    __writeImpl(IceInternal.BasicStream __os)
    {
        __os.startWriteSlice("::speeral::SpeeralError", -1, true);
        __os.writeString(reason);
        __os.endWriteSlice();
    }

    protected void
    __readImpl(IceInternal.BasicStream __is)
    {
        __is.startReadSlice();
        reason = __is.readString();
        __is.endReadSlice();
    }

    public static final long serialVersionUID = 4225059655119280848L;
}

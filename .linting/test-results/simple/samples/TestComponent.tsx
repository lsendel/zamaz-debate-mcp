import React from 'react';
export const TestComponent = (props) => {
    const [data,setData] = React.useState([]);
    return <div>{props.title}</div>;
};

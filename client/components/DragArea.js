import React from 'react'
import { render } from 'react-dom'

import HTML5Backend from 'react-dnd-html5-backend';
import { DragDropContext } from 'react-dnd';

//TODO: czy nie da sie tego zrobic jakos inaczej??
class DragArea extends React.Component {
  render() {
    return (<div>
      {this.props.children}
    </div>)

  }
}
export default DragDropContext(HTML5Backend)(DragArea)

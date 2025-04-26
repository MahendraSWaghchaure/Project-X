// const Home = () => {
//     return (
//       <div className="home-page">
//         <h1>Welcome to School Bus Tracking System</h1>
//         <p>Real-time student safety for schools and parents.</p>
//       </div>
//     );
//   };
  
//   export default Home;
  

import { Link } from 'react-router-dom';

const Home = () => {
  return (
    <div className="home-page">
      <h1>Welcome to the School Bus Tracking System</h1>
      <p>Ensuring student safety with real-time bus tracking for schools and parents.</p>
      <div className="buttons">
        <Link to="/about">Learn More</Link> | <Link to="/contact">Contact Us</Link>
      </div>
    </div>
  );
};

export default Home;
